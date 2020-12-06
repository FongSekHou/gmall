package com.atguigu.gmall.payment.mq.listener;

import com.atguigu.gmall.service.PaymentService;
import org.mockito.internal.verification.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.TextMessage;
import java.util.Map;

@Component
public class PaymentServiceMqListener {
    @Autowired
    PaymentService paymentService;


    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentCheck(MapMessage mapMessage) throws JMSException {
        String orderSn = mapMessage.getString("orderSn");
        int times = mapMessage.getInt("times");
        Map<String, Object> resultMap = paymentService.checkAlipayPayment(orderSn);
        if(resultMap.size()!=0){
            String trade_no = (String) resultMap.get("trade_no");
            String out_trade_no = (String)resultMap.get("out_trade_no");
            String total_amount = (String)resultMap.get("total_amount");
            String call_back_content = (String)resultMap.get("call_back_content");
            paymentService.updatePayment(out_trade_no,trade_no,call_back_content,total_amount);
        }else {//若是仍然没有支付，则再次发送检查消息
            if(times>0){
                times--;
                paymentService.sendAlipayPaymentCheck(orderSn, times);
            }else {
                //查询不到支付信息，记录异常日志
            }

        }
    }

}
