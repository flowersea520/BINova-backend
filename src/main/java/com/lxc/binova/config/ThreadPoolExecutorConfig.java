package com.lxc.binova.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  这个配置类：就是将 THreadPoolExecutor线程池对象，注入到我们spring的容器中
 * @author mortal
 * @date 2024/6/12 16:07
 */
@Configuration
public class ThreadPoolExecutorConfig {
	// 实现了ThreadFactory接口的匿名内部类，
	// 我可以理解为，通过接口创建了其实现类，然后匿名内部类的逻辑，就是对应实现类的逻辑
// 创建了一个实现了ThreadFactory接口的匿名内部类，并覆盖了newThread()方法。
// 其作用是为线程池创建新的线程，同时自定义了线程的命名规则。
	@Bean
	// 线程池需要一个线程工厂来创建新线程。这样，线程池实际上将线程工厂作为其内部组成部分来管理线程的创建过程。
	public ThreadPoolExecutor threadPoolExecutor() {
		// 创建员工的工厂，决定员工的各种属性。
		// 作用：线程工厂主要用来创建线程对象。它提供了一种将线程的创建和配置进行抽象的方式，让你可以自定义线程的创建过程，如设置线程的名称、优先级等。
		//使用场景：线程工厂通常用在线程池中，当线程池需要创建新的线程时，会调用线程工厂的newThread()方法来创建线程。
		ThreadFactory threadFactory = new ThreadFactory() {
			private int count = 1;
			// 线程工厂根据需要创建新的线程，并给每个线程分配一个唯一的名称（类似工号），确保每个线程都有自己独特的标识。
			@Override
			// Runnable 是 Java 提供的一个函数式接口，只包含一个抽象方法 void run()。
			// 它用于表示可以被其他线程执行的任务
			// 在线程工厂中，你需要实现 newThread(Runnable r) 方法来创建一个新的线程并返回。
			// 传入的 Runnable r 就是表示要在这个新线程中执行的任务。
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r); // 使用传入的 Runnable 创建线程
				thread.setName("线程：" + count);
				count ++;
				return thread;
			}
		};

		// 线程等待队列为一个容量为 4 的阻塞队列。
		/**
		 * 如果在创建 ThreadPoolExecutor 时没有明确指定拒绝策略，则会使用默认的拒绝策略。
		 * ThreadPoolExecutor 的默认拒绝策略是 AbortPolicy。
		 *
		 * 默认拒绝策略（AbortPolicy）
		 * AbortPolicy 是 ThreadPoolExecutor 中提供的一种拒绝策略，具体行为是：
		 *
		 * 当任务提交到线程池时，如果线程池已达到其最大容量（包括核心线程、最大线程和任务队列均已满），则会拒绝新的任务。
		 * 该策略将抛出一个未检查的 RejectedExecutionException，通知调用者任务不能被接受。
		 */
		return new ThreadPoolExecutor(2, 4, 100, TimeUnit.MINUTES,
//				这个异步任务的线程是由线程池中的线程工厂创建的
				new ArrayBlockingQueue<>(4), threadFactory,
				new ThreadPoolExecutor.AbortPolicy() //当任务队列满时抛出 RejectedExecutionException
				);
	}
}
