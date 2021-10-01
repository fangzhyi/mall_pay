package com.joker.pay.service.impl;

import com.joker.pay.PayApplicationTests;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.listener.endpoint.JmsActivationSpecFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.jms.Queue;
import java.math.BigDecimal;
import java.util.UUID;


public class PayServiceTest extends PayApplicationTests {

    @Autowired
    PayService payService;

    private Queue queue = new ActiveMQQueue("pay_queue");

    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Test
    public void testCreate() {

        payService.create("1111117041221321",new BigDecimal("0.01"), BestPayTypeEnum.WXPAY_NATIVE);
    }
    @Test
    public void sendMQMsg(){

        jmsMessagingTemplate.convertAndSend( queue, "pay"+ UUID.randomUUID().toString().substring(0,6));
    }
}