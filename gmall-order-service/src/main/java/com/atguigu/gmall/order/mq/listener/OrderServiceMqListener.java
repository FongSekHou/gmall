package com.atguigu.gmall.order.mq.listener;

import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import javax.jms.JMSException;
import javax.jms.TextMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;


    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentSuccess(TextMessage textMessage) throws JMSException {

        String out_trade_no = textMessage.getText();
        orderService.updateOrderStatus(out_trade_no);

    }
}