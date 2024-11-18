package com.lxc.binova.manager;

import com.lxc.binova.common.ErrorCode;
import com.lxc.binova.exception.BusinessException;
import org.redisson.api.*;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
@Service
public class RateLimiterManager {

	/**
	 * 这段代码就是注入了RedissonClient对象, 使用构造器注入，将这个redissonClient，注入到我们的属性当中去了
	 *
	 * @param
	 */
//    @Autowired
//    public RateLimiterManager(RedissonClient redissonClient) {
//        this.redissonClient = redissonClient;
//    }

	@Resource
	private RedissonClient redissonClient;

	private static final String RATE_LIMITER_PREFIX = "userRateLimiter:";
	//速率”直接定义了每秒（或其他时间单位）可以获取的“许可”数量，
	// “速率”实际上定义了“许可”的生成速度。在限流器的内部，会有一个机制按照这个速率来生成“许可”。
	// todo 如果“速率”设置得较低，那么相同时间内生成的“许可”数量就会减少，从而限制操作的执行频率。
	private static final int RATE = 5; // 每秒允许的最大请求数

	/**
	 * 限流操作
	 *
	 * @param key 区分不同的限流器，比如不同的用户id 应该分别统计
	 */
	public void doRateLimit(String key) {
		String rateLimiterKey = RATE_LIMITER_PREFIX + key;
		// 创建了一个限流器对象，我们给他起了一个独有名字（创建了独有的key，这个key是指定字符串前缀 + userId）
		RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);
		// 初始化限流器，每秒不超过5个请求，超过后拒绝
		// RateType.OVERALL：指定总体速率限制，即所有用户的总体速率限制。
		// RATE：设置每秒允许的 最大 请求数量。（这里是5）
		// 设置速率限制的时间间隔，这里表示时间间隔为1秒。
		// RateIntervalUnit.SECONDS：指定时间间隔单位为秒。
		rateLimiter.trySetRate(
				RateType.OVERALL,
				RATE,
				1,
				RateIntervalUnit.SECONDS
		);
		// 尝试获取权限 （要令牌，每次要一个令牌), 每当一个操作来了之后，请求一个令牌
		// “许可”是限流器用于控制单个操作能否执行的权限或令牌，
		// 注意：刚开始测试那个循环是一秒执行，但是那个for 5次循环的时候：只执行2次，即使我每次请求的许可是1个许可
		// rate是4个请求    但是仍只有两个请求成功，是因为：tryAcquire有时候许可可能获取不到
		// 当一个请求到达限流器时，它会尝试获取一个许可。如果许可可用，则请求被允许，许可被消耗，并继续处理下一个请求。
		// 如果许可不可用（例如，由于其他线程已经获取了所有可用的许可），则请求将被拒绝或延迟。
		boolean canOp = rateLimiter.tryAcquire(1);
		// 如果返回false，表示不能请求了（没有获取到令牌）
		if (!canOp) {
			// 抛一个自定义的请求状态码，表示请求太过频繁了
			throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
		}

	}
}