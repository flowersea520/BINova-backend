package com.lxc.binova.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 *
 * @author mortal
 * @date 2024/4/23 15:56
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redisson")
@Data // 提供了可以保证安全性，不提供 getter 和 setter 方法，可以直接访问属性，不安全
public class RedissonConfig {
//	Spring Boot 会将 YAML 文件中配置的属性值映射到这个私有属性上。
	private String address;
	private String password;

	@Bean
	public RedissonClient redissonClient() {
		// 1. 创建 redisson 配置对象
		Config config = new Config();
		// 地址在yml中，我们读取yml的配置文件，映射到了我们的Java对象中
		// useSingleServer() 是配置 Redisson 使用单个 Redis 服务器的方法
		config.useSingleServer().setAddress(address).setPassword(password).setDatabase(1);
		// 2. Create Redisson instance
		// Sync and Async API:：这种方式创建的 Redisson 实例支持同步和异步的操作。
		// 你可以使用同步方法进行阻塞式的操作，也可以使用异步方法进行非阻塞式的操作。
		RedissonClient redisson = Redisson.create(config);
		return redisson;
	}
}