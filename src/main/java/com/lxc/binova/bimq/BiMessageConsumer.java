package com.lxc.binova.bimq;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.lxc.binova.common.ErrorCode;
import com.lxc.binova.exception.BusinessException;
import com.lxc.binova.manager.SparkManager;
import com.lxc.binova.model.entity.Chart;
import com.lxc.binova.service.ChartService;
import com.lxc.binova.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author mortal
 * @date 2024/7/1 0:38
 */
@Component
@Slf4j
public class BiMessageConsumer {
	@Resource
	private SparkManager sparkManager;

	@Resource
	private ChartService chartService;


	// 指定程序的消息队列，和确认机制
//	这是 Lombok 提供的注解，用于在方法中抑制受检异常的报告，通过抛出 Exception 类型的异常来隐藏具体的异常信息。
	@SneakyThrows
	//queues = {} 表示监听器监听的队列，这里应该填写具体的队列名称。
	//ackMode = "MANUAL" 指定了消息的确认模式为手动确认模式。
	@RabbitListener(queues = {BiMqContant.BI_QUEUE}, ackMode = "MANUAL")
	// 注意：这个时候接收的message消息就是 图表id，我们将其转换为long类型，然后查数据库
	public void receviceMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
		log.info("receviceMessage = {}", message);
		// 判断消息是否为空
		if (StrUtil.isBlank(message)) {
			// 为空，将消息拒绝掉，然后丢掉，空消息没必要重新放入队列
			channel.basicNack(deliveryTag, false, false);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
		}
		Long chartId = Convert.toLong(message);
		Chart chart = chartService.getById(chartId);
		// 判断图表是否为空
		if (ObjUtil.isEmpty(chart)) {
			// 拒绝这个消息，然后抛异常
			channel.basicNack(deliveryTag, false, false);
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
		}

		// 我们向ai调用 生成图表的时候，是异步的，所以在异步方法执行的过程中，我们可以再次更新数据库的数据，
		// 将图表的status属性，变为 running，给前端看，表示正在 生成的过程中
		// 图表生成成功，则保存 succeed； 生成失败，则保存为 failed，记录任务失败的信息
		Chart updateChart = new Chart();
		updateChart.setId(chart.getId());
		updateChart.setStatus("running");
		boolean b = chartService.updateById(updateChart);
		// 如果 更新图表 执行状态 失败
		if (!b) {
			channel.basicNack(deliveryTag, false, false);
			handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
			return;
		}

		// 调用星火ai，将其输入的内容传入ai模型中去
		String result = sparkManager.sendMesToAIUseXingHuo(buildUserInput(chart));
		// 我们将ai生成的结果以：【【【【【划分，（因为这是我们charts图表代码，单独抽取出来给前端）
		//		split() 方法用于将一个字符串根据指定的分隔符（正则表达式）分割成一个字符串数组。
		String[] splits = result.split("【【【【【");
		// 我们刚刚在测试类里面看了，被【【【【【这个拆分出来有三块，所以我们可以给其一个条件，就是
		// 如果拆分之后小于三块，那么就ai生成异常
		if (splits.length < 3) {
			channel.basicNack(deliveryTag, false, false);
			handleChartUpdateError(chart.getId(), "AI生成错误");
			return;
		}

		//  "星火AI返回的结果【【【【【前端代码【【【【【分析结论";
		// 0号元素，通过debug发现是 空格
		// 所以1号元素就是 前端图表charts代码
		// 这里做个优化， 去除genChart和genResult的首尾空格  制表符 和 换行符 等空白字符，使用字符串的trim方法
		// Java中的trim()方法可以 去除 字符串 首尾 的空格，包括 空格 、制表符 和 换行符 等空白字符
		String genChart = splits[1].trim();
		// 2号元素就是我们的结论代码
		String genResult = splits[2].trim();
		// 程序执行到这里就代表生成图表成功了，修改状态到数据库中（不用保存到数据库，因为刚开始保存了），
		// 把succeed状态弄进去，还有把生成的图表和结论
		Chart updateChartResult = new Chart();
		updateChartResult.setId(chart.getId());
		updateChartResult.setGenChart(genChart);
		updateChartResult.setGenResult(genResult);
		updateChartResult.setStatus("succeed");
		boolean updateResult = chartService.updateById(updateChartResult);
		if (!updateResult) {
			channel.basicNack(deliveryTag, false, false);
			handleChartUpdateError(chart.getId(), "更新图表状态失败");
		}
		// 确认消息  deliveryTag 是用来标识消息的标签，false 表示确认单条消息。
		// 确认消息之前会执行 原本线程池中的任务代码逻辑
		channel.basicAck(deliveryTag, false);
	}

	/**
	 *  构建用户输入
	 * @param chart
	 * @return
	 */
	private String buildUserInput(Chart chart) {
		String goal = chart.getGoal();
		String chartType = chart.getChartType();
		String csvData = chart.getChartData();

		//	 stringBuilder中的append方法是 拼接字符串的；全部拼接在 字符串构造器中
		// 这里记得换行，让自己的数据更加的清晰一些
		StringBuilder userInput = new StringBuilder();
		userInput.append("分析需求").append("\n");


		// 拼接分析目标 (加上更详细的图表类型需求给ai）
		String userGoal = goal;
		if (StrUtil.isNotBlank(chartType)) {
			userGoal += "请使用：" + chartType;
		}

		// 例如前端输入的分析目标：详细分析网站用户的增长情况
		userInput.append(userGoal).append("\n");

		// 读取到用户上传的excel文件，进行一个处理
		// CSV 文件通常是纯文本文件，每行代表一条记录，字段之间用逗号分隔
		// 使用excel自定义的工具类，将excel转换为：csv格式（以下部分就是result）
		// 日期,用户数
		//1号,10个
		//2号,20个
		//3号,30个
		userInput.append("原始数据：").append("\n");
		// 这里就是压缩后的数据（注意：这个result对象，已经在excelToCsv加了换行符了）
		userInput.append(csvData).append("\n");
		return userInput.toString();
	}


	/**
	 * 当好几个地方要处理错误，我们可以把他单独抽离出来一个方法
	 * @param chartId 传过来 更新数据库 图表错误的id
	 * @param execMessage 传过来 执行的错误信息
	 */
	private void handleChartUpdateError(long chartId, String execMessage) {
		Chart updateChartResult = new Chart();
		updateChartResult.setId(chartId);
		updateChartResult.setExecMessage(execMessage);
		// 根据 传过来的图表错误的 chartId，我们将数据库的信息 修改，将对应的status修改为 failed
		updateChartResult.setStatus("failed");
		boolean b = chartService.updateById(updateChartResult);
		if (!b) {
			log.error("更新图表失败后更新对应的status失败" + chartId + ", " +  execMessage);
		}


	}

}
