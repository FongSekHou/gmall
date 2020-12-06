package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(String out_trade_no,String trade_no,String call_back_content,String total_amount);

    void sendPaymentSuccess(String out_trade_no);

    void sendAlipayPaymentCheck(String orderSn,int times);

    Map<String, Object> checkAlipayPayment(String orderSn);
}
