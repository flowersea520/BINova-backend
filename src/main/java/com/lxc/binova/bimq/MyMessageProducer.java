package com.lxc.binova.bimq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author mortal
 * @date 2024/7/1 0:28
 */
@Component
public class MyMessageProducer {
	@Resource
	private RabbitTemplate rabbitTemplate;

	public void sendMessage(String exchange, String routeKey, String message) {
		rabbitTemplate.convertAndSend(exchange, routeKey, message);
	}




}
