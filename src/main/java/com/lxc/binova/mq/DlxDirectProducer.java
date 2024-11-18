package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

public class DlxDirectProducer {
    // 死信 交换机（转换接收死信的，然后将死信的消息转发到 死信队列上）
  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
    // 这个就是接收正常消息的交换机（工作交换机）
    // 记住：消费者一定要和生产者绑定同一个交换机（这样才处于同一个模型中）
    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        // 声明死信交换机 Dead Letter exchange
        channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");
        // todo 其实创建队列是在消费者中 创建的（在这里创建也行，只是说消费者创建更加规范)
        // 老板的死信队列
        String queueName = "laoban_dlx_queue";
        // 创建消息队列
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, DEAD_EXCHANGE_NAME, "laoban");
        // 外包公司的死信队列
        String queueName2 = "waibao_dlx_queue";
        // 创建消息队列
        channel.queueDeclare(queueName2, true, false, false, null);
        channel.queueBind(queueName2, DEAD_EXCHANGE_NAME, "waibao");

        // 死信队列：老板 处理消息的逻辑
        DeliverCallback laobanDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // 我们获取这个消息的标签，然后拒绝【这里拒绝就是为了查看 死信的效果，是否会放到死信的队列中去】
            // 小李的死信是给 外包的死信队列
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [老板] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        // 死信队列：外包 处理消息的逻辑
        DeliverCallback waibaoDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // 我们获取这个消息的标签，然后拒绝【这里拒绝就是为了查看 死信的效果，是否会放到死信的队列中去】
            // 小李的死信是给 外包的死信队列
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [外包] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        // 这里收的是 小王queueName队列的死信，所以用laoban的死信处理逻辑
        channel.basicConsume(queueName, false, laobanDeliverCallback, consumerTag -> {
        });
        // 这里收的是 小李queueName2队列的死信，所以用外包的死信处理逻辑
        channel.basicConsume(queueName2, false, waibaoDeliverCallback, consumerTag -> {
        });





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
            // 发消息（记住: 发消息肯定是给工作 交换机发消息，他会发给正常的工作队列）
            // 千万别给死信队列发消息
            channel.basicPublish(WORK_EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "':'" + routingKey + "'");
        }

    }
  }
}