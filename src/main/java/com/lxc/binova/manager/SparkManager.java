package com.lxc.binova.manager;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建一个SparkManager类（星火ai管理器），用来调用星火AI，
 * 在这里我们让AI扮演一名数据分析师，根据我们的输入，做出预设的反应：
 */
@Component
@Slf4j
public class SparkManager {
	@Resource
	private SparkClient sparkClient;

	/**
	 * AI生成问题的预设条件
	 */
	public static final String PRECONDITION = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\" +\n" +
			"        \"分析需求：\" +\n" +
			"        \"{数据分析的需求或者目标}\" +\n" +
			"        \"原始数据：\" +\n" +
			"        \"{csv格式的原始数据，用,作为分隔符}\" +\n" +
			"        \"请根据这两部分内容，参考这个json前端模板，生成和其结构相似的前端echarts代码：{\n" +
			"    \"title\": {\n" +
			"        \"text\": \"网站用户增长情况\"\n" +
			"    },\n" +
			"    \"xAxis\": {\n" +
			"        \"type\": \"category\",\n" +
			"        \"data\": [\"1号\", \"2号\", \"3号\"]\n" +
			"    },\n" +
			"    \"yAxis\": {\n" +
			"        \"type\": \"value\"\n" +
			"    },\n" +
			"    \"series\": [{\n" +
			"        \"dataName\": \"用户数\",\n" +
			"        \"data\": [10, 20, 30],\n" +
			"        \"type\": \"line\"\n" +
			"    }]\n" +
			"}" +
			"按照以下指定格式生成内容：（此外不要输出任何多余的开头、结尾、注释）\" +\n" +
			"        \"【【【【【\" +\n" +
			"        \"{前端 Echarts V5 的 option 配置对象json代码(不用包含option=)，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\" +\n" +
			"        \"【【【【【\" +\n" +
			"        \"{明确的数据分析结论，越详细越好，不要生成多余的注释，注意一定要符合json的格式，避免报错分析失败：after property name in JSON ，符合生成eChart的格式要求}\" +\n" +
			"        \"最终格式是：【【【【【{前端json代码展示}【【【【【分析结论";

	/**
	 * 向星火AI发送请求
	 * 官网的同步调用（看这个就好了：https://github.com/briqt/xunfei-spark4j）
	 *
	 * @param content
	 * @return
	 */
	public String sendMesToAIUseXingHuo(final String content) {
		// 消息列表，可以在此列表添加历史对话记录
		List<SparkMessage> messages = new ArrayList<>();
		messages.add(SparkMessage.systemContent(PRECONDITION));
		messages.add(SparkMessage.userContent(content));
		// 构造请求
		SparkRequest sparkRequest = SparkRequest.builder()
				// 消息列表
				.messages(messages)
				// 模型回答的tokens的最大长度，非必传，默认为2048
				.maxTokens(2048)
				// 结果随机性，取值越高随机性越强，即相同的问题得到的不同答案的可能性越高，非必传，取值为[0,1]，默认为0.5
				.temperature(0.2)
				// 指定请求版本
				.apiVersion(SparkApiVersion.V3_5)
				.build();
		// 同步调用
		SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
		String responseContent = chatResponse.getContent();
		log.info("星火AI返回的结果{}", responseContent);
		return responseContent;
	}
}