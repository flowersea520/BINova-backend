package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

	private static final String TASK_QUEUE_NAME = "multi_queue";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");

		// 代码放到 for 循环中是一种模拟多个消费者并发处理消息的方式
		for (int i = 0; i < 2; i++) {
			final Connection connection = factory.newConnection();
			final Channel channel = connection.createChannel();
            //durable设置为true后，重启服务器队列不丢失
			channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
			System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        // 设置了每次从队列中接收消息的最大数量为 1。这样做可以确保在处理完一个消息之前不会接收下一个消息，实现了负载均衡。
        // 设置消息质量，一次只 接收 一条消息
			channel.basicQos(1);
        // 创建了一个 DeliverCallback 对象，它是一个函数式接口，
        // 用于定义当消息被消费时执行的操作。
        // 具体来说，这个回调函数会在接收到消息时被调用。（delivery发送，传递的意思）
        // 定义了如何处理消息（先执行下面的消费监听方法，然后才会走这个回调）
			int finalI = i;
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				// delivery.getBody() 获取消息的内容
				String message = new String(delivery.getBody(), "UTF-8");
				try {
					// 处理工作
					System.out.println(" [x] Received '" + "编号：" + finalI + "消息" + message + "'");
					// 确认消息机制：ack 和 unack未确认
					// delivery.getEnvelope().getDeliveryTag():
					// 这部分代码是用来获取消息的传递标识（Delivery Tag），用于唯一标识一个消息。
					// multiple这个参数表示是否批量确认消息，当设置为 false 时，表示只确认当前消息，而不是一次性确认多条消息。
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
					// 每个消息发送过来之后：停20秒，模拟 机器处理能力有限（例如：一个功能就干20秒)
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					// nack 方法 是用于向 RabbitMQ 明确拒绝一条或多条消息的方法。
					// requeue指定是否将 被拒绝的消息 重新放回队列，如果设置为 true，消息将被重新放回队列；如果设置为 false，消息将被丢弃或进入死信队列。
					channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
				} finally {
					System.out.println(" [x] Done");
					// 在处理完消息后发送确认，告诉 RabbitMQ 这个消息已经处理完毕。
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				}
			};
			// 这行代码开始消费【接收】指定队列的消息。
			// 开启消费监听,
			// 先执行消费监听方法basicConsume，然后当有消息到达时，才会调用 deliverCallback 方法来处理消息。
			channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
			});
		}
	}


}