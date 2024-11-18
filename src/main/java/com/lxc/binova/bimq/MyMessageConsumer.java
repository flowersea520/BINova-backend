package com.lxc.binova.bimq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author mortal
 * @date 2024/7/1 0:38
 */
@Component
@Slf4j
public class MyMessageConsumer {
	// 指定程序的消息队列，和确认机制
//	这是 Lombok 提供的注解，用于在方法中抑制受检异常的报告，通过抛出 Exception 类型的异常来隐藏具体的异常信息。
	@SneakyThrows
	//queues = {} 表示监听器监听的队列，这里应该填写具体的队列名称。
	//ackMode = "MANUAL" 指定了消息的确认模式为手动确认模式。
	@RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
	public void receviceMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
		log.info("receviceMessage = {}", message);
		// 确认消息  deliveryTag 是用来标识消息的标签，false 表示确认单条消息。
		channel.basicAck(deliveryTag, false);
	}

}
