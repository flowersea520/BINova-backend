package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {
	// 死信 交换机（转换接收死信的，然后将死信的消息转发到 死信队列上）
	private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";

	// 这个就是接收正常消息的交换机（工作交换机）
	// 记住：消费者一定要和生产者绑定同一个交换机（这样才处于同一个模型中）
	private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		// 创建一个 Channel 对象并绑定到连接（Connection）是表示与 RabbitMQ 通信的重要步骤。
		// 使用 Channel 对象，你可以在 RabbitMQ 中执行各种操作，如声明交换机、声明队列、发布消息和消费消息等。
		Channel channel = connection.createChannel();

		// 创建交换机（注意：和生产者的一样，才能接收到）
		channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");

		// 指定死信队列参数
		Map<String, Object> args = new HashMap();
		// 要绑定 到 那个交换机
		args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
		// 指定死信 要转发到哪个队列（这里用的direct直接交换机，则指定路由键，将其转发到指定的消息队列中去
		args.put("x-dead-letter-routing-key", "laoban");

		/**
		 *  小王队列中 出现死信给 ”老板“
		 * 当队列中的消息变成死信后，会被发送到名为 DEAD_EXCHANGE_NAME 的交换机，
		 * 并且被路由到路由键为 "waibao" 的队列中。【死信处理】
		 */
		// 两个消息队列
		String queueName = "小王的任务队列";
		// 注意：绑定消息队列之前，一定要 创建 消息队列
		//  arg 参数 用于设置 队列 的其他属性
		// ，args 参数中的这些属性设置与具体的消息队列 queueName 是相关联的，
		// 用于定义queueName队列的死信队列配置，以便在消息变成死信时进行相应的处理。
		channel.queueDeclare(queueName, true, false, false, args);
		// 路由键（Routing Key）是用于将消息从交换机（Exchange）路由到队列（Queue）的重要参数
		// 将消息队列绑定到交换机上，然后fanout交换机一广播，就会发送到绑定的所有的 消息队列 上面了
		channel.queueBind(queueName, WORK_EXCHANGE_NAME, "xiaoWang");


		/**
		 *  小李队列中 出现死信给 ”外包“
		 */
		// 指定死信队列参数
		Map<String, Object> args2 = new HashMap();
		// 要绑定 到 那个交换机
		args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
		// 指定死信 要转发到哪个队列（这里用的direct直接交换机，则指定路由键，将其转发到指定的消息队列中去）
		args2.put("x-dead-letter-routing-key", "waibao");

		// 两个消息队列
		String queueName2 = "小李的任务队列";
		// 注意：绑定消息队列之前，一定要 创建 消息队列
		// args2将这个 死信的配置，引用到小李 的任务队列当中去
		channel.queueDeclare(queueName2, true, false, false, args2);
		channel.queueBind(queueName2, WORK_EXCHANGE_NAME, "xiaoLi");

		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		// 员工小王取 消息的 操作逻辑
		DeliverCallback xiaoWangDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			// 我们获取这个消息的标签，然后拒绝【这里拒绝就是为了查看 死信的效果，是否会放到死信的队列中去】
			// 小王的死信是给 老板的死信队列
			channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
			System.out.println(" [小王] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};

		// 员工小李取 消息的 操作逻辑
		DeliverCallback xiaoLiDeliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			// 我们获取这个消息的标签，然后拒绝【这里拒绝就是为了查看 死信的效果，是否会放到死信的队列中去】
			// 小李的死信是给 外包的死信队列
			channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
			System.out.println(" [小李] Received '" +
					delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};



		// 接收消息
		channel.basicConsume(queueName, false, xiaoWangDeliverCallback, consumerTag -> {
		});

		// 接收消息（接收指定队列的消息，这里接收指定的queueName2小李的任务队列）
		channel.basicConsume(queueName2, false, xiaoLiDeliverCallback, consumerTag -> {
		});
	}
}