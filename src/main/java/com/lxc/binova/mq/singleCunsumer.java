package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

/**
 * 消费者：就是接收消息的
 *  生产者和交换机相关，因为生产者要将消息发送到 交换机，然后由交换机 将消息转发到 消息队列 来存储；
 *  然后消费者就是 从消息队列中取消息
 *  路由：就是 将消息 转发到指定的 消息队列上面【例如将包裹放到指定的快递站】
 */
public class singleCunsumer {
//  记住：队列名一定要一样（保证发送的消息在一个队列上）-
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        // 创建连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 创建队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        // 定义了如何处理消息
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
        };
        // 消费消息，会持续堵塞【消费消息，就是接收消息】
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }
}