package com.lxc.binova.mq;

import com.rabbitmq.client.*;

public class DirectConsumer {

	// 记住：消费者一定要和生产者绑定同一个交换机（这样才处于同一个模型中）
	private static final String EXCHANGE_NAME = "direct_exchange";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		// 创建一个 Channel 对象并绑定到连接（Connection）是表示与 RabbitMQ 通信的重要步骤。
		// 使用 Channel 对象，你可以在 RabbitMQ 中执行各种操作，如声明交换机、声明队列、发布消息和消费消息等。
		Channel channel = connection.createChannel();

		// 创建交换机（注意：和生产者的一样，才能接收到）
		channel.exchangeDeclare(EXCHANGE_NAME, "direct");
		// 两个消息队列
		String queueName = "小王的任务队列";
		// 注意：绑定消息队列之前，一定要 创建 消息队列
		channel.queueDeclare(queueName, true, false, false, null);
		// 路由键（Routing Key）是用于将消息从交换机（Exchange）路由到队列（Queue）的重要参数
		// 将消息队列绑定到交换机上，然后fanout交换机一广播，就会发送到绑定的所有的 消息队列 上面了
		channel.queueBind(queueName, EXCHANGE_NAME, "xiaoWang");

		// 两个消息队列
		String queueName2 = "小李的任务队列";
		// 注意：绑定消息队列之前，一定要 创建 消息队列
		channel.queueDeclare(queueName2, true, false, false, null);
		// 路由键（Routing Key）是用于将消息从交换机（Exchange）路由到队列（Queue）的重要参数
		// 将消息队列绑定到交换机上，然后fanout交换机一广播，就会发送到绑定的所有的 消息队列 上面了
		// 生产者发消息要带路由键, 如果是xiaoLi，则发到queueName2小李的任务队列里面
		channel.queueBind(queueName2, EXCHANGE_NAME, "xiaoLi");

		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		// 员工取 消息的 操作逻辑
		DeliverCallback xiaoWangDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [小王] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};

		// 员工取 消息的 操作逻辑
		DeliverCallback xiaoLiDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(" [小李] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};

		// 接收消息
		channel.basicConsume(queueName, true, xiaoWangDeliverCallback, consumerTag -> {
		});

		// 接收消息（接收指定队列的消息，这里接收指定的queueName2小李的任务队列）
		channel.basicConsume(queueName2, true, xiaoLiDeliverCallback, consumerTag -> {
		});
	}
}