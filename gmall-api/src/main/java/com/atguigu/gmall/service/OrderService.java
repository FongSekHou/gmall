package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

public interface OrderService {
    String generateTradeCode(String memberId);

    boolean checkTradeCode(String tradeCode,String memberId);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByMemberId(String memberId);

    void updateOrderStatus(String out_trade_no);

    OmsOrder getOmsOrderByOrderSn(String orderSn);
}
