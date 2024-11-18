package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * 生产者: 就是发送消息的；
 * 生产者：发消息到某个交换机
 * 消费者：从某个队列中取消息
 * 交换机（Exchange）：负责把消息 转发 到对应的队列（交换机负责接收消息并根据某些规则决定将消息发送到哪些队列）
 * 队列（Queue）：存储消息的
 * 路由（Routes）：转发，就是怎么把消息从一个地方转到另一个地方（比如从生产者转发到某个队列）
 */
public class singleProducer {

	private final static String QUEUE_NAME = "hello";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		// 建立连接、创建频道
		try (Connection connection = factory.newConnection();
			 // 通道也可以叫频道
			 // 通道（Channel）代表着与 RabbitMQ 服务器的连接通道，
			 // 在该通道上您可以进行消息的发送和接收。通常"通道"用来表示客户端与消息队列之间的通信通道，类似于通信的管道。
			 Channel channel = connection.createChannel()) {
			// 创建消息队列
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			String message = "Hello World!";
			// 发送消息
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
			System.out.println(" [x] Sent '" + message + "'");
		}
	}
}