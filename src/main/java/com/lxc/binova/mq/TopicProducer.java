package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

public class TopicProducer {

  private static final String EXCHANGE_NAME = "topic_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String userInput = scanner.nextLine();
            // 将用户输入的内容，空格隔开，然后放到数组里面去
            String[] strings = userInput.split(" ");
            // 对这个数组进行判断
            if (strings.length < 1) {
                continue;
            }
            // 第一个元素我们当消息（以空格分隔)
            String message = strings[0];
            // 第二个元素，我们弄路由键
            String routingKey = strings[1];
            // 发消息
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "':'" + routingKey + "'");
        }
    }
  }
}