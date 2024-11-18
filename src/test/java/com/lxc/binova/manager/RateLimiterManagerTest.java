package com.lxc.binova.manager;

import cn.hutool.core.thread.ThreadUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mortal
 * @date 2024/6/10 23:42
 */
@SpringBootTest
class RateLimiterManagerTest {

	@Resource
	private RateLimiterManager rateLimiterManager;

	@Test
	void testDoRateLimit() {
		// 限流器的后缀名
		String userId = "1";
		// 注意：这个for循环不是用来衡量程序或服务每秒能够处理的请求数量的。
		// 它只是用来执行一定次数的循环操作，例如在循环中执行某些操作或处理数据。
		for (int i = 0; i < 1; i++) {
			rateLimiterManager.doRateLimit(userId);
			System.out.println("请求成功");
		}

		// todo 这里让其睡上 1秒，然后一秒后执行（下一次限流）
		ThreadUtil.sleep(1000);

		// 使用循环来测试限流器是因为循环提供了一种简单的方式来模拟大量的请求。
		for (int i = 0; i < 5; i++) {
			// 把for当成用户点击，
			// 正常情况下，执行5次，但是我们限流了，也就是说：用户每秒内限制用户最多执行 rate 次请求。
			// 当然要分 能获取许可的情况
			rateLimiterManager.doRateLimit(userId);
			// 每次循环结束时，都会输出一条消息表示获取请求成功。表示用户成功获取了请求许可，可以执行请求了。
			System.out.println("获取请求成功");
		}


	}
}