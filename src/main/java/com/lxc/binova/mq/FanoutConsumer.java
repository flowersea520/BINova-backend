package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class FanoutConsumer {
  private static final String EXCHANGE_NAME = "fanout-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    // 创建交换机（注意：和生产者的一样，才能接收到）
    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
    // 两个消息队列
    String queueName = "小王的任务队列";
    // 注意：绑定消息队列之前，一定要 创建 消息队列
    channel.queueDeclare(queueName, true, false, false, null);
    // 路由键（Routing Key）是用于将消息从交换机（Exchange）路由到队列（Queue）的重要参数
    // 将消息队列绑定到交换机上，然后fanout交换机一广播，就会发送到绑定的所有的 消息队列 上面了
    channel.queueBind(queueName, EXCHANGE_NAME, "");

    String queueName2 = "小李的任务队列";
    // 注意：绑定消息队列之前，一定要 创建 消息队列
    channel.queueDeclare(queueName2, true, false, false, null);
    // 将消息队列绑定到交换机上，然后fanout交换机一广播，就会发送到绑定的所有的 消息队列 上面了
    channel.queueBind(queueName2, EXCHANGE_NAME, "");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
    // 处理接收的消息【关于 消息队列 1的处理消息逻辑】
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [小王] Received '" + message + "'");
    };
    // 处理接收的消息【关于 消息队列 2的处理消息逻辑】
    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      System.out.println(" [小李] Received '" + message + "'");
    };
    // 消费监听（接收消息）  -- 这里接收消息队列1的消息
    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    // 接收消息队列2 的消息
    channel.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });
  }
}