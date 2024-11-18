package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class TopicConsumer {

	private static final String EXCHANGE_NAME = "topic_exchange";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		// 三个消息队列
		String queueName = "frontend_queue";
		channel.queueDeclare(queueName, true, false, false, null);
//		topic 交换机  （绑定关系：可以模糊匹配多个绑定）  * 号匹配一个单词，
		// todo 这个路由键设计要有些小心思的，就是同时兼容 “前端和后端”的消息队列
		// "#" 用于匹配零个或多个单词。（所以我发消息：前端.后端) 都能收到
		channel.queueBind(queueName, EXCHANGE_NAME, "#.前端.#");

		// 三个消息队列
		String queueName2 = "backend_queue";
		// 注意：绑定消息队列之前，一定要 创建 消息队列
		channel.queueDeclare(queueName2, true, false, false, null);
		// todo 这个路由键设计要有些小心思的，就是同时兼容 “前端和后端”的消息队列
		channel.queueBind(queueName2, EXCHANGE_NAME, "#.后端.#");
		// 三个消息队列
		String queueName3 = "product_queue";
		// 注意：绑定消息队列之前，一定要 创建 消息队列
		channel.queueDeclare(queueName3, true, false, false, null);

		channel.queueBind(queueName3, EXCHANGE_NAME, "*.产品.*");

		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		// 处理消息（员工小a处理消息的逻辑）
		DeliverCallback xiaoaDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [小a] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};

		// 处理消息（员工小b处理消息的逻辑）
		DeliverCallback xiaobDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [小b] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};

		// 处理消息（员工小c处理消息的逻辑）
		DeliverCallback xiaocDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [小c] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};

		// 管道接收消息；（三个员工，分别接收三个不同的消息队列的消息）
		channel.basicConsume(queueName, true, xiaoaDeliverCallback, consumerTag -> {
		});
		channel.basicConsume(queueName2, true, xiaobDeliverCallback, consumerTag -> {
		});
		channel.basicConsume(queueName3, true, xiaocDeliverCallback, consumerTag -> {
		});
	}
}