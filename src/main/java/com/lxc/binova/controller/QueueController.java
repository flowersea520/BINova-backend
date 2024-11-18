package com.lxc.binova.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *  线程池测试 接口
 */
@RestController
@RequestMapping("/queue")
@Slf4j
// 指定在dev和本地的环境生效
@Profile({"dev", "local"})
public class QueueController {

	// 注入我们的 配置类中配置好的 threadPoolExecutor线程池对象
	@Resource
	private ThreadPoolExecutor threadPoolExecutor;

	/**
	 * Executor 接口是用于执行提交的任务的对象，并且 Executor 接口本身不是一个线程池对象。
	 * 但是，Executor 接口的实现类 ThreadPoolExecutor 是一个常用的线程池实现。
	 * @param name
	 *  当你向这个 /add 接口发送一个 POST 请求，比如传入参数 name=John，系统会立即返回响应，告诉你请求已经收到。
	 *  然后在后台有一个任务开始执行，打印出 "任务执行中John"，然后等待60秒，之后完成任务。
	 *  这使得系统能够处理其他请求，同时后台任务在后台慢慢完成。
	 */
	@PostMapping("/add")
	// add(String name) 方法中启动了一个异步任务，异步任务会在后台执行一些操作，
	// 但方法本身不会等待异步任务的完成。这种设计可以让系统更高效地处理耗时操作，提升性能和并发能力。
	public void add(String name) {
		// CompletableFuture 专门用于实现异步编程和并发编程。这个类不仅可以简化异步操作的实现，
		// 还可以用来管理多个异步任务
		// runAsync 是异步方法：，你可以方便地实现异步执行任务，异步运行一个不返回结果的任务。
		// 任务：表示要多什么事情
		CompletableFuture.runAsync(() -> {
			System.out.println("任务执行中" + name + "执行人：" + Thread.currentThread().getName());
			try {
				// Thread.sleep(60000); 在这里是用来模拟耗时操作，让您能够观察异步执行的效果。
				// 异步执行本身的意义在于提高系统的并发能力、改善用户体验和优化资源利用，
				// 让系统能够更高效地处理多个任务而不阻塞主线程。
				Thread.sleep(6000000); // 睡眠之后，正式工会执行完这个，然后执行完之后正式工就会看任务队列【备忘录】里面的任务
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, threadPoolExecutor);
	}

	@GetMapping("/get")
	public String get() {
		// 创建一个map集合，将 关于线程池的 属性放到 map集合中
		HashMap<String, Object> map = new HashMap<>();
		int size = threadPoolExecutor.getQueue().size();
		map.put("队列长度", size);
		long taskCount = threadPoolExecutor.getTaskCount();
		map.put("任务总数: ", taskCount);
		long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
		// 如果我们让其 在完成的过程中，睡眠了，说明在睡眠之内都完成不了，这个任务数就是0
		map.put("已完成任务数：", completedTaskCount);
		int activeCount = threadPoolExecutor.getActiveCount();
		map.put("正在工作的线程数：" , activeCount);
		return JSONUtil.toJsonStr(map);
	}



}
