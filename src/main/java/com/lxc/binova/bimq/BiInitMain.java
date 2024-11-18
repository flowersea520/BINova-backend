package com.lxc.binova.bimq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前，执行一次）
 *
 * @author mortal
 * @date 2024/7/1 0:50
 */
public class BiInitMain {

	public static void doInit() {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("localhost");
			Connection connection = factory.newConnection();
			// 创建一个 Channel 对象并绑定到连接（Connection）是表示与 RabbitMQ 通信的重要步骤。
			// 使用 Channel 对象，你可以在 RabbitMQ 中执行各种操作，如声明交换机、声明队列、发布消息和消费消息等。
			Channel channel = connection.createChannel();
			final String EXCHANGE_NAME = BiMqContant.BI_EXCHANGENAME;
			// 创建交换机（注意：和生产者的一样，才能接收到）
			channel.exchangeDeclare(EXCHANGE_NAME, "direct");

			String queueName = BiMqContant.BI_QUEUE;
			// 注意：绑定消息队列之前，一定要 创建 消息队列
			channel.queueDeclare(queueName, true, false, false, null);
			// 路由键（Routing Key）是用于将消息从交换机（Exchange）路由到队列（Queue）的重要参数
			// 将消息队列绑定到交换机上，然后fanout交换机一广播，就会发送到绑定的所有的 消息队列 上面了
			channel.queueBind(queueName, EXCHANGE_NAME, BiMqContant.BI_ROUTINGKEY);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static void main(String[] args) {
		doInit();

	}


}
