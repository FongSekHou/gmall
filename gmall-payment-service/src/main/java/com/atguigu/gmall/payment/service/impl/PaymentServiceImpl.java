package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.constant.MqConst;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.utils.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePayment(String out_trade_no,String trade_no,String call_back_content,String total_amount) {
        //幂等性检查：先根据orderSn查询该支付信息是否被修改过，若已被修改过则直接return，否则继续
        PaymentInfo info = new PaymentInfo();
        info.setOrderSn(out_trade_no);
        info = paymentInfoMapper.select(info).get(0);//应该检查info集合的数量，只有一个元素或没有元素才正常
        if (info.getPaymentStatus().equals("已付款")) {
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(out_trade_no);
        paymentInfo.setAlipayTradeNo(trade_no);
        paymentInfo.setCallbackContent(call_back_content);
        paymentInfo.setTotalAmount(new BigDecimal(total_amount));
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setPaymentStatus("已付款");
        Example example = new Example(PaymentInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderSn", paymentInfo.getOrderSn());
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    @Override
    public void sendPaymentSuccess(String out_trade_no) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection conn = null;
        Session session = null;
        MessageProducer producer = null;
        try{
            conn = connectionFactory.createConnection();
            conn.start();
            session = conn.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue(MqConst.getPaymentSuccessQueue());
            producer = session.createProducer(queue);
            TextMessage message = new ActiveMQTextMessage();
            message.setText(out_trade_no);
            producer.send(message);
            session.commit();
        }catch (Exception e){
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }finally {
            try {
                producer.close();
                session.close();
                conn.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void sendAlipayPaymentCheck(String orderSn,int times) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection conn = null;
        Session session = null;
        MessageProducer producer = null;
        try{
            conn = connectionFactory.createConnection();
            session = conn.createSession(true,Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue(MqConst.getPaymentCheckQueue());
            producer = session.createProducer(queue);
            MapMessage message = new ActiveMQMapMessage();
            message.setString("orderSn",orderSn);
            message.setInt("times",times);
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*30);//设置消息的延迟属性，30秒发一次
            producer.send(message);
            session.commit();
        }catch (Exception e){
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }finally {
            try {
                producer.close();
                session.close();
                conn.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> checkAlipayPayment(String orderSn) {
        Map<String,Object> resultMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no",orderSn);
        request.setBizContent(JSON.toJSONString(requestMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);//内部远程调用，在service层中取到外部服务器的值
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("有可能交易已创建，调用成功");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            resultMap.put("call_back_content",response.getMsg());//response.getMsg获得的不是callBackContain
            resultMap.put("total_amount",response.getTotalAmount());
        } else {
            System.out.println("有可能交易未创建，调用失败");
        }

        return resultMap;
    }

}
