package com.lxc.binova.bimq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author mortal
 * @date 2024/7/1 0:28
 */
@Component
public class BiMessageProducer {
	@Resource
	private RabbitTemplate rabbitTemplate;

	/**
	 *  发送消息
	 * @param message
	 */
	public void sendMessage(String message) {
		rabbitTemplate.convertAndSend(BiMqContant.BI_EXCHANGENAME, BiMqContant.BI_ROUTINGKEY, message);
	}
}
