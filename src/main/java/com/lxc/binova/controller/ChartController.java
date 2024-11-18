package com.lxc.binova.controller;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.binova.annotation.AuthCheck;
import com.lxc.binova.bimq.BiMessageProducer;
import com.lxc.binova.common.BaseResponse;
import com.lxc.binova.common.DeleteRequest;
import com.lxc.binova.common.ErrorCode;
import com.lxc.binova.common.ResultUtils;
import com.lxc.binova.constant.CommonConstant;
import com.lxc.binova.constant.FileConstant;
import com.lxc.binova.constant.UserConstant;
import com.lxc.binova.exception.BusinessException;
import com.lxc.binova.exception.ThrowUtils;


import com.lxc.binova.manager.RateLimiterManager;
import com.lxc.binova.manager.SparkManager;
import com.lxc.binova.model.dto.chart.*;

import com.lxc.binova.model.dto.file.UploadFileRequest;
import com.lxc.binova.model.entity.Chart;

import com.lxc.binova.model.entity.User;

import com.lxc.binova.model.enums.FileUploadBizEnum;
import com.lxc.binova.model.vo.BiResponse;
import com.lxc.binova.service.ChartService;
import com.lxc.binova.service.UserService;
import com.lxc.binova.utils.ExcelUtils;
import com.lxc.binova.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

	// 调用 星火ai的
	@Resource
	private SparkManager sparkManager;

	@Resource
	private ChartService chartService;

	@Resource
	private UserService userService;

	@Resource
	private RateLimiterManager rateLimiterManager;

	@Resource
	private ThreadPoolExecutor threadPoolExecutor;

	@Resource
	private BiMessageProducer biMessageProducer;

	// region 增删改查

	/**
	 * 创建
	 *
	 * @param chartAddRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/add")
	public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
		if (chartAddRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = new Chart();
		BeanUtils.copyProperties(chartAddRequest, chart);

		User loginUser = userService.getLoginUser(request);
		chart.setUserId(loginUser.getId());

		boolean result = chartService.save(chart);
		ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
		long newChartId = chart.getId();
		return ResultUtils.success(newChartId);
	}

	/**
	 * 删除
	 *
	 * @param deleteRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/delete")
	public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
		if (deleteRequest == null || deleteRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		User user = userService.getLoginUser(request);
		long id = deleteRequest.getId();
		// 判断是否存在
		Chart oldChart = chartService.getById(id);
		ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
		// 仅本人或管理员可删除
		if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
		}
		boolean b = chartService.removeById(id);
		return ResultUtils.success(b);
	}

	/**
	 * 更新（仅管理员）
	 *
	 * @param chartUpdateRequest
	 * @return
	 */
	@PostMapping("/update")
	@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
		if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = new Chart();
		BeanUtils.copyProperties(chartUpdateRequest, chart);
		long id = chartUpdateRequest.getId();
		// 判断是否存在
		Chart oldChart = chartService.getById(id);
		ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
		boolean result = chartService.updateById(chart);
		return ResultUtils.success(result);
	}

	/**
	 * 根据 id 获取
	 *
	 * @param id
	 * @return
	 */
	@GetMapping("/get")
	public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
		if (id <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = chartService.getById(id);
		if (chart == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
		}
		return ResultUtils.success(chart);
	}


	/**
	 * 分页获取列表（封装类）
	 *
	 * @param chartQueryRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/list/page")
	public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
													 HttpServletRequest request) {
		long current = chartQueryRequest.getCurrent();
		long size = chartQueryRequest.getPageSize();
		// 限制爬虫
		ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
		Page<Chart> chartPage = chartService.page(new Page<>(current, size),
				getQueryWrapper(chartQueryRequest));
		return ResultUtils.success(chartPage);
	}

	/**
	 * 分页获取当前用户创建的资源列表
	 *  其实就多了一个 userId的条件
	 * @param chartQueryRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/my/list/page")
	public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
													   HttpServletRequest request) {
		if (chartQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		// 分页获取当前用户创建的资源列表 的关键步骤
		chartQueryRequest.setUserId(loginUser.getId());
		// 这个就是前端searchParams对象过来的两个当前页和 每页记录数的属性，我们从接收的dto中取出来
		long current = chartQueryRequest.getCurrent();
		long size = chartQueryRequest.getPageSize();
		// 限制爬虫
		ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
		// mybatisplus要分页，关键就是 插件page对象的两个参数：current和pageSize
		// 分页操作本质上是一种查询操作，只不过它会限制结果集的范围，按照一页一页的数据返回。
		// 同时，你也能够在分页查询中添加查询条件，以满足特定的查询需求。
		// 执行分页查询时通过传入 QueryWrapper 对象来添加条件限制，这样可以在执行分页操作的同时对数据进行进一步筛选。
		Page<Chart> chartPage = chartService.page(new Page<>(current, size),
				getQueryWrapper(chartQueryRequest));
		return ResultUtils.success(chartPage);
	}

	// endregion


	/**
	 * 编辑（用户）
	 *
	 * @param chartEditRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/edit")
	public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
		if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = new Chart();
		BeanUtils.copyProperties(chartEditRequest, chart);

		User loginUser = userService.getLoginUser(request);
		long id = chartEditRequest.getId();
		// 判断是否存在
		Chart oldChart = chartService.getById(id);
		ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
		// 仅本人或管理员可编辑
		if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
		}
		boolean result = chartService.updateById(chart);
		return ResultUtils.success(result);
	}

	/**
	 * 智能分析 （同步）
	 *
	 * @param multipartFile       表示要上传的文件。
	 * @param genChartByAiRequest 生成图表类型 请求
	 *
	 * @return
	 */
	@PostMapping("/genChart")
	public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
												 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
		// 图表名称
		String name = genChartByAiRequest.getName();
		// 分析目标
		String goal = genChartByAiRequest.getGoal();
		// 图表类型
		String chartType = genChartByAiRequest.getChartType();

		User loginUser = userService.getLoginUser(request);

		// 拿到dto的属性后，我们立刻进行属性校验（非空）
		// 条件成立则抛异常（调用这个工具类）
		ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
		ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称为空");
		ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

		// 校验文件，关于mutilfile文件
//		使用 getSize() 方法可以获得文件的大小，以字节为单位
		long size = multipartFile.getSize();
		// 使用 getOriginalFilename() 方法可以获取文件的原始名称，即上传时的文件名。
		String originalFilename = multipartFile.getOriginalFilename();
		// 定义一个 1MB的常量
		final long ONE_MB = 1024 * 1024L;
		// 这里就是校验文字的大小
		ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");

		// 校验文件名的后缀
		String suffix = FileUtil.getSuffix(originalFilename);
		// 指定 非法的文件扩展后缀，将其放入到我们的集合当中（说白了只能传 excel，然后方便我们转csv）
		final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
		ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");


		// 限流判断，防止用户 疯狂刷量，导致服务器崩溃，或者是 自己破产
		// 我们做的策略是：每秒内，让用户最多请求5次，  每当一个请求操作来了之后：
		// 就：获取一个令牌（令牌为true可用，则请求通过）
		// -- 不要总去想他怎么获取令牌的，我们就管好我们的每秒最大请求次数就好了
		rateLimiterManager.doRateLimit(StrUtil.toString(loginUser.getId()));



		// 这里就没有必要写prompt了，我们直接调用星火ai接口，使用注入的SparkManger
		// 因为我们用
		//  这个 在SparkManger已经配置了prompt提示词了，所以我们就可以直接用了（输入要给ai的内容即可）
// todo 这里拼接用户输入的内容
		/**
		 * 分析需求：
		 * 分析网站用户的增长情况 请使用 + chartType： （这个goal目标从前端传过来的请求对象中获取）
		 * 原始数据：
		 * 日期，用户数  (excel文件中的东西，要转csv）
		 * 1号，10
		 * 2号，20
		 * 3号，30
		 */
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
		String csvData = ExcelUtils.excelToCsv(multipartFile);
		userInput.append(csvData).append("\n");

		// 调用星火ai，将其输入的内容传入ai模型中去
		String result = sparkManager.sendMesToAIUseXingHuo(userInput.toString());
		// 我们将ai生成的结果以：【【【【【划分，（因为这是我们charts图表代码，单独抽取出来给前端）
//		split() 方法用于将一个字符串根据指定的分隔符（正则表达式）分割成一个字符串数组。
		String[] splits = result.split("【【【【【");
		// 我们刚刚在测试类里面看了，被【【【【【这个拆分出来有三块，所以我们可以给其一个条件，就是
		// 如果拆分之后小于三块，那么就ai生成异常
		if (splits.length < 3) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI生成错误");
		}

		//  "星火AI返回的结果【【【【【前端代码【【【【【分析结论";
		// 0号元素，通过debug发现是 空格
		// 所以1号元素就是 前端图表charts代码
		// 这里做个优化， 去除genChart和genResult的首尾空格  制表符 和 换行符 等空白字符，使用字符串的trim方法
		// Java中的trim()方法可以 去除 字符串 首尾 的空格，包括 空格 、制表符 和 换行符 等空白字符
		String genChart = splits[1].trim();
		// 2号元素就是我们的结论代码
		String genResult = splits[2].trim();
		// 响应给前端数据之前，我们一定要保存到数据库中
		Chart chart = new Chart();
		chart.setName(name);
		chart.setGoal(goal);
//		图表数据 (会传 excel转换后 的 csv类型）
		chart.setChartData(csvData);
		chart.setChartType(chartType);

		chart.setGenChart(genChart);
		chart.setGenResult(genResult);
		// 登录的用户id
		chart.setUserId(loginUser.getId());
		boolean saveResult = chartService.save(chart);
		ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表生成错误");
		// 我们将这两个结果单独封装一个响应类中去返回给前端
		BiResponse biResponse = new BiResponse();
		biResponse.setGenChart(genChart);
		biResponse.setGenResult(genResult);
		// 将这个新生成的图表id，传给前端
		biResponse.setChartId(chart.getId());
		return ResultUtils.success(biResponse);


	}


	/**
	 * 智能分析 (异步）
	 * 生成图表（注意：文件上传功能不要手动去写，很麻烦，用别人的模板）
	 *
	 * @param multipartFile       表示要上传的文件。
	 * @param genChartByAiRequest 生成图表类型 请求
	 * @param request
	 * @RequestPart("file") 注解指定了要从前端请求中获取的特定部分的名称为 "file"，
	 * @return
	 */
	@PostMapping("/genChart/async")
	public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
											 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
		// 图表名称
		String name = genChartByAiRequest.getName();
		// 分析目标
		String goal = genChartByAiRequest.getGoal();
		// 图表类型
		String chartType = genChartByAiRequest.getChartType();

		User loginUser = userService.getLoginUser(request);

		// 拿到dto的属性后，我们立刻进行属性校验（非空）
		// 条件成立则抛异常（调用这个工具类）
		ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
		ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称为空");
		ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

	// 校验文件，关于mutilfile文件
//		使用 getSize() 方法可以获得文件的大小，以字节为单位
		long size = multipartFile.getSize();
		// 使用 getOriginalFilename() 方法可以获取文件的原始名称，即上传时的文件名。
		String originalFilename = multipartFile.getOriginalFilename();
		// 定义一个 1MB的常量
		final long ONE_MB = 1024 * 1024L;
		// 这里就是校验文字的大小
		ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");

		// 校验文件名的后缀
		String suffix = FileUtil.getSuffix(originalFilename);
		// 指定 非法的文件扩展后缀，将其放入到我们的集合当中（说白了只能传 excel，然后方便我们转csv）
		final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
		ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");


		// 限流判断，防止用户 疯狂刷量，导致服务器崩溃，或者是 自己破产
		// 我们做的策略是：每秒内，让用户最多请求5次，  每当一个请求操作来了之后：
		// 就：获取一个令牌（令牌为true可用，则请求通过）
		// -- 不要总去想他怎么获取令牌的，我们就管好我们的每秒最大请求次数就好了
		rateLimiterManager.doRateLimit(StrUtil.toString(loginUser.getId()));



		// 这里就没有必要写prompt了，我们直接调用星火ai接口，使用注入的SparkManger
		// 因为我们用
		//  这个 在SparkManger已经配置了prompt提示词了，所以我们就可以直接用了（输入要给ai的内容即可）
// todo 这里拼接用户输入的内容
		/**
		 * 分析需求：
		 * 分析网站用户的增长情况 请使用 + chartType： （这个goal目标从前端传过来的请求对象中获取）
		 * 原始数据：
		 * 日期，用户数  (excel文件中的东西，要转csv）
		 * 1号，10
		 * 2号，20
		 * 3号，30
		 */
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
		String csvData = ExcelUtils.excelToCsv(multipartFile);
		userInput.append(csvData).append("\n");
		// todo 这里是个优化：就是在没有调用ai之前，我们保存数据库的信息（除了生成的图表，和生成的结论）
		// todo 新增的 两个字段 status 任务状态 ，记得保存到数据库中，给用户看
		// todo 后面所有的chart实体都是为了更新 Chart chart = new Chart() 的 status状态的
		Chart chart = new Chart();
		chart.setName(name);
		chart.setGoal(goal);
//		图表数据 (会传 excel转换后 的 csv类型）
		chart.setChartData(csvData);
		chart.setChartType(chartType);
		// 可以将其改造为枚举值
		chart.setStatus("wait");
		// 登录的用户id
		chart.setUserId(loginUser.getId());
		boolean saveResult = chartService.save(chart);
		ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表生成错误");

		try {
			// 异步调用ai（不会让用户一直等）
			CompletableFuture.runAsync(() -> {
				// 我们向ai调用 生成图表的时候，是异步的，所以在异步方法执行的过程中，我们可以再次更新数据库的数据，
				// 将图表的status属性，变为 running，给前端看，表示正在 生成的过程中
				// 图表生成成功，则保存 succeed； 生成失败，则保存为 failed，记录任务失败的信息
				Chart updateChart = new Chart();
				updateChart.setId(chart.getId());
				updateChart.setStatus("running");
				boolean b = chartService.updateById(updateChart);
				// 如果 更新图表 执行状态 失败
				if (!b) {
					handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
					return;
				}

				// 调用星火ai，将其输入的内容传入ai模型中去
				String result = sparkManager.sendMesToAIUseXingHuo(userInput.toString());
				// 我们将ai生成的结果以：【【【【【划分，（因为这是我们charts图表代码，单独抽取出来给前端）
	//		split() 方法用于将一个字符串根据指定的分隔符（正则表达式）分割成一个字符串数组。
				String[] splits = result.split("【【【【【");
				// 我们刚刚在测试类里面看了，被【【【【【这个拆分出来有三块，所以我们可以给其一个条件，就是
				// 如果拆分之后小于三块，那么就ai生成异常
				if (splits.length < 3) {
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
					handleChartUpdateError(chart.getId(), "更新图表状态失败");
				}
			}, threadPoolExecutor); //  将异步任务 提交 给自定义线程池执行
		} catch (RejectedExecutionException e) {
			// 当任务队列满了且临时线程也满了，抛出异常时的处理逻辑
			System.err.println("任务队列已满，请稍后再试");
		}
		// 我们将这两个结果单独封装一个响应类中去返回给前端
		BiResponse biResponse = new BiResponse();
		// 将这个新生成的图表id，传给前端（代码异步的, 不在同一个线程当中，所以当BiResponse这个线程走到这里的时候
		// 估计异步代码的线程还没走完，所以获取不到
		// 所以我无法将生成的chart图表和结论放到这个响应对象中去）
		biResponse.setChartId(chart.getId());
		return ResultUtils.success(biResponse);
	}


	/**
	 * 智能分析 (异步消息队列）
	 * 生成图表（注意：文件上传功能不要手动去写，很麻烦，用别人的模板）
	 *
	 * @param multipartFile       表示要上传的文件。
	 * @param genChartByAiRequest 生成图表类型 请求
	 * @param request
	 * @RequestPart("file") 注解指定了要从前端请求中获取的特定部分的名称为 "file"，
	 * @return
	 */
	@PostMapping("/genChart/async/mq")
	public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
													  GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
		// 图表名称
		String name = genChartByAiRequest.getName();
		// 分析目标
		String goal = genChartByAiRequest.getGoal();
		// 图表类型
		String chartType = genChartByAiRequest.getChartType();

		User loginUser = userService.getLoginUser(request);

		// 拿到dto的属性后，我们立刻进行属性校验（非空）
		// 条件成立则抛异常（调用这个工具类）
		ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
		ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称为空");
		ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

		// 校验文件，关于mutilfile文件
//		使用 getSize() 方法可以获得文件的大小，以字节为单位
		long size = multipartFile.getSize();
		// 使用 getOriginalFilename() 方法可以获取文件的原始名称，即上传时的文件名。
		String originalFilename = multipartFile.getOriginalFilename();
		// 定义一个 1MB的常量
		final long ONE_MB = 1024 * 1024L;
		// 这里就是校验文字的大小
		ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");

		// 校验文件名的后缀
		String suffix = FileUtil.getSuffix(originalFilename);
		// 指定 非法的文件扩展后缀，将其放入到我们的集合当中（说白了只能传 excel，然后方便我们转csv）
		final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
		ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");


		// 限流判断，防止用户 疯狂刷量，导致服务器崩溃，或者是 自己破产
		// 我们做的策略是：每秒内，让用户最多请求5次，  每当一个请求操作来了之后：
		// 就：获取一个令牌（令牌为true可用，则请求通过）
		// -- 不要总去想他怎么获取令牌的，我们就管好我们的每秒最大请求次数就好了
		rateLimiterManager.doRateLimit(StrUtil.toString(loginUser.getId()));



		// 这里就没有必要写prompt了，我们直接调用星火ai接口，使用注入的SparkManger
		// 因为我们用
		//  这个 在SparkManger已经配置了prompt提示词了，所以我们就可以直接用了（输入要给ai的内容即可）
// todo 这里拼接用户输入的内容
		/**
		 * 分析需求：
		 * 分析网站用户的增长情况 请使用 + chartType： （这个goal目标从前端传过来的请求对象中获取）
		 * 原始数据：
		 * 日期，用户数  (excel文件中的东西，要转csv）
		 * 1号，10
		 * 2号，20
		 * 3号，30
		 */
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
		String csvData = ExcelUtils.excelToCsv(multipartFile);
		userInput.append(csvData).append("\n");
		// 这里是个优化：就是在没有调用ai之前，我们保存数据库的信息（除了生成的图表，和生成的结论）
		//  新增的 两个字段 status 任务状态 ，记得保存到数据库中，给用户看
		//  后面所有的chart实体都是为了更新 Chart chart = new Chart() 的 status状态的
		Chart chart = new Chart();
		chart.setName(name);
		chart.setGoal(goal);
//		图表数据 (会传 excel转换后 的 csv类型）
		chart.setChartData(csvData);
		chart.setChartType(chartType);
		// 可以将其改造为枚举值
		chart.setStatus("wait");
		// 登录的用户id
		chart.setUserId(loginUser.getId());
		boolean saveResult = chartService.save(chart);
		ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表生成错误");
		// （这里我们将任务放到消费者代码中，而不是将任务放到线程池当中）
		// 异步调用ai（不会让用户一直等）
		// 发消息，将图表id发送到消费者当中，然后消费者执行对应的逻辑
		Long newChartId = chart.getId();
		biMessageProducer.sendMessage(StrUtil.toString(newChartId));

		// 我们将这两个结果单独封装一个响应类中去返回给前端
		BiResponse biResponse = new BiResponse();
		// 将这个新生成的图表id，传给前端（代码异步的, 不在同一个线程当中，所以当BiResponse这个线程走到这里的时候
		// 估计异步代码的线程还没走完，所以获取不到
		// 所以我无法将生成的chart图表和结论放到这个响应对象中去）
		biResponse.setChartId(newChartId);
		return ResultUtils.success(biResponse);
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





	/**
	 * 获取查询 条件 方法
	 *
	 * @param chartQueryRequest
	 * @return
	 */

	private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {


		QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
		if (chartQueryRequest == null) {
			return queryWrapper;
		}

		Long id = chartQueryRequest.getId();
		String name = chartQueryRequest.getName();
		String goal = chartQueryRequest.getGoal();
		String chartType = chartQueryRequest.getChartType();
		Long userId = chartQueryRequest.getUserId();
		int current = chartQueryRequest.getCurrent();
		int pageSize = chartQueryRequest.getPageSize();
		String sortField = chartQueryRequest.getSortField();
		String sortOrder = chartQueryRequest.getSortOrder();

		// 拼接查询条件
		queryWrapper.eq(id != null && id > 0, "id", id);
		queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
		queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
		queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
		queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
		// 查询根据 没有被删除的（查询的就是Chart实体，因为该方法的返回结果是：QueryWrapper<Chart> ）
		queryWrapper.eq("isDelete", false);

		queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
		queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
//      进行排序操作时，实际上是在指定数据库查询结果的返回顺序。（也是一种查询操作）
		// 如果前两个参数都返回 false，系统会忽略排序操作，
		// 不会抛出错误，也不会影响查询的执行或结果。代码会继续执行但是查询结果将不会按任何特定方式排序。
		// 如果第二个参数为false是不是降序排
		queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
				sortField);
		return queryWrapper;
	}
}
