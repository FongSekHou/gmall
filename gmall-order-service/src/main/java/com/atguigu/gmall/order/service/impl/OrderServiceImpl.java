package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.constant.MqConst;
import com.atguigu.gmall.constant.RedisConst;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.utils.ActiveMQUtil;
import com.atguigu.gmall.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String generateTradeCode(String memberId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeCode = UUID.randomUUID().toString();
        try {
            jedis.setex(RedisConst.getTradeCodeKey(memberId), 60 * 5, tradeCode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("generateTradeCode fail");
        } finally {
            jedis.close();
        }
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String tradeCode, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        boolean success = false;
        Long flag = null;
        try {
            String key = RedisConst.getTradeCodeKey(memberId);
            flag = (Long) jedis.eval(RedisConst.getLuaScriptToDelLock(), Collections.singletonList(key), Collections.singletonList(tradeCode));//使用lua脚本迅速删除
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("generateTradeCode fail");
        } finally {
            jedis.close();
        }
        if (flag == 1) {
            success = true;
        }
        return success;
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        omsOrderMapper.insertSelective(omsOrder);
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(omsOrder.getId());
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
        //写不写入缓存视具体情况而定
    }

    @Override
    public OmsOrder getOrderByMemberId(String memberId) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setMemberId(memberId);
        omsOrder.setStatus(0);//订单状态要是未支付订单
        List<OmsOrder> omsOrders = omsOrderMapper.select(omsOrder);
        if (omsOrders.size() == 0) {
            return null;
        }else if (omsOrders.size()==1){
            return omsOrders.get(0);
        }else {
            throw new RuntimeException("More than one order detected based on memberId.");
        }
    }

    @Override
    public void updateOrderStatus(String out_trade_no) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setStatus(1);
        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",out_trade_no);
        omsOrderMapper.updateByExampleSelective(omsOrder,example);
        //修改完订单状态后发送消息锁定库存
        OmsOrder order = getOmsOrderByOrderSn(out_trade_no);
        sendOrderPaySuccess(JSON.toJSONString(order));
    }

    @Override
    public OmsOrder getOmsOrderByOrderSn(String orderSn) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(orderSn);
        List<OmsOrder> omsOrders = omsOrderMapper.select(omsOrder);
        if(omsOrders.size()==0){
            return null;
        }else if(omsOrders.size()==1){
            OmsOrder order = omsOrders.get(0);
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            omsOrderItem.setOrderId(order.getId());
            List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);
            order.setOmsOrderItems(omsOrderItems);
            return order;
        }else {
            throw new RuntimeException("more than one order by one orderSn.");
        }
    }

    private void sendOrderPaySuccess(String omsOrder) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection conn = null;
        Session session = null;
        MessageProducer producer = null;
        try{
            conn = connectionFactory.createConnection();
            session = conn.createSession(true,Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue(MqConst.getOrderPayQueue());
            producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage();
            message.setText(omsOrder);
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
}
