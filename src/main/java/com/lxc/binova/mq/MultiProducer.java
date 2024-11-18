package com.lxc.binova.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.util.Scanner;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

public class MultiProducer {

  private static final String TASK_QUEUE_NAME = "multi_queue";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         // 创建频道（管道）
         Channel channel = connection.createChannel()) {
        // 消息持久化（第二个参数为true）
        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);

//        String message = String.join(" ", argv);
        Scanner scanner = new Scanner(System.in);
        // while 用于检查：用户是否继续输入，如果输入，这个while就会执行
        // 当用户按下回车键时，Scanner 会检查当前行是否有输入。如果有，hasNext() 会返回 true
        while (scanner.hasNext()) {
            // nextLine一行一行读；    next逐个读取以空格分隔的单词
            String message = scanner.nextLine();
            // 发消息，发到指定的队列名（这里还指定了消息的属性persistent持久化）
            channel.basicPublish("", TASK_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
  }

}