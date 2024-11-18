package com.lxc.binova.mapper;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mortal
 * @date 2024/6/10 0:36
 */
@SpringBootTest
class ChartMapperTest {
	@Resource
	private ChartMapper chartMapper;

	@Test
	void queryChartData() {
		String chartId = "1799360261386395649";
		// String类中的format方法是用来将字符串格式化输出的。
		// 这个方法允许你创建带有格式标记的字符串，类似于C语言中的printf函数。
		String querysql = String.format("select 用户数 from chart_%s", chartId);
		List<Map<String, Object>> chartData = chartMapper.queryChartData(querysql);
		System.out.println(chartData);

	}}