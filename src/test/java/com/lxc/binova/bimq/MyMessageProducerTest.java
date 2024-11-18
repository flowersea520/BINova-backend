package com.lxc.binova.bimq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mortal
 * @date 2024/7/1 1:08
 */
@SpringBootTest
class MyMessageProducerTest {

	@Resource
	private MyMessageProducer myMessageProducer;

	@Test
	void sendMessage() {
		myMessageProducer.sendMessage(BiMqContant.BI_EXCHANGENAME, BiMqContant.BI_ROUTINGKEY, "你好呀宝宝");
	}
}