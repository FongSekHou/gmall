package com.atguigu.gmall.constant;

public class MqConst {

    /**
     * PAYMENT_CHECK_QUEUE
     * 1 支付完成(支付服务)
     * PAYHMENT_SUCCESS_QUEUE
     * 2 订单已支付(订单服务)
     * ORDER_PAY_QUEUE
     * 3 库存锁定(库存系统)
     * SKU_DEDUCT_QUEUE
     * 4 订单已出库(订单服务)
     * ORDER_SUCCESS_QUEUE
     */

    public static final String getPaymentCheckQueue(){
        return "PAYMENT_CHECK_QUEUE";
    }
    public static final String getPaymentSuccessQueue(){
        return "PAYMENT_SUCCESS_QUEUE";
    }
    public static final String getOrderPayQueue(){
        return "ORDER_PAY_QUEUE";
    }
    public static final String getSkuDeductQueue(){
        return "SKU_DEDUCT_QUEUE";
    }
    public static final String getOrderSuccessQueue(){
        return "ORDER_SUCCESS_QUEUE";
    }

}

