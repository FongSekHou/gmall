package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;
    @Autowired
    AlipayClient alipayClient;
    @Reference
    PaymentService paymentService;

    @RequestMapping("/index")
    @LoginRequired
    public String index(ModelMap map, HttpServletRequest request) {
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        OmsOrder omsOrder = orderService.getOrderByMemberId(memberId);
        if (omsOrder != null) {
            map.put("outTradeNo", omsOrder.getOrderSn());
            map.put("totalAmount", omsOrder.getTotalAmount());
        }
        map.put("nickname", nickname);
        return "index";
    }

    /**
     * 1. 生成表单
     * 2. 生成支付信息并存入数据库
     * 3. 将表单返回前端
     *
     * @param request
     * @param modelMap
     * @return
     */
    @RequestMapping("/alipay/submit")
    @LoginRequired
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, ModelMap modelMap) {
        String memberId = (String) request.getAttribute("memberId");
        OmsOrder omsOrder = orderService.getOrderByMemberId(memberId);
        String orderSn;
        BigDecimal totalAmount;
        String subject = "感光徕卡Pro300随便命名系列手机";
        String form = null;
        if (omsOrder != null) {
            orderSn = omsOrder.getOrderSn();
            totalAmount = omsOrder.getTotalAmount();
            //制作请求支付宝的表单信息，提供支付宝需要的数据
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

            // 封装参数进request
            alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);//同步回调地址
            alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//异步回调地址
            Map<String, Object> map = new HashMap<>();
            map.put("out_trade_no", orderSn);//外部订单号
            map.put("product_code", "FAST_INSTANT_TRADE_PAY");//支付宝规定，只能写死
            map.put("total_amount", 0.01);//总价
            map.put("subject", subject);//订单名
            String param = JSON.toJSONString(map);
            alipayRequest.setBizContent(param);
            try {
                form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
                System.out.println(form);
            } catch (AlipayApiException e) {
                e.printStackTrace();
            }
            //制作请求支付宝的表单信息，提供支付宝需要的数据
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setCreateTime(new Date());
            paymentInfo.setOrderId(omsOrder.getId());
            paymentInfo.setOrderSn(orderSn);
            paymentInfo.setPaymentStatus("未付款");
            paymentInfo.setSubject(subject);
            paymentService.savePaymentInfo(paymentInfo);

            //用户到达支付宝提页面后，发送延时队列，查询交易状态
            paymentService.sendAlipayPaymentCheck(orderSn,5);

        }
        return form;
    }

    /**
     * 1. 获取参数验签
     * 2. 更新订单
     * 3. 通知其他服务
     * 4. 回调请求中获取支付宝参数
     *
     * @param request
     * @return
     */
    @RequestMapping("/alipay/callback/return")
    @LoginRequired
    public String callbackReturn(HttpServletRequest request) {

        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String total_amount = request.getParameter("total_amount");
        String call_back_content = request.getQueryString();//获得查询字符串，即get请求问号后的全部参数

        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if (StringUtils.isNotBlank(sign)) {
            // 验签成功
            // 更新用户的支付状态
           /* PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);// 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setTotalAmount(new BigDecimal(total_amount));*/
            paymentService.updatePayment(out_trade_no,trade_no,call_back_content,total_amount);

            //发送消息更新订单状态至 1待发货
            paymentService.sendPaymentSuccess(out_trade_no);
        }

        return "finish";
    }

    @RequestMapping("/wx/submit")
    @LoginRequired
    @ResponseBody
    public String wxSubmit() {
        return "微信支付功能尚未开通";
    }

    /**
     * @param request
     * @param modelMap
     * @return
     */
    @LoginRequired
    @RequestMapping("/fake/alipay/submit")
    public String fakeAlipaySubmit(HttpServletRequest request, ModelMap modelMap) throws UnsupportedEncodingException {
        String memberId = (String) request.getAttribute("memberId");
        OmsOrder omsOrder = orderService.getOrderByMemberId(memberId);
        String orderSn = null;
        BigDecimal totalAmount = null;
        String subject = "感光徕卡Pro300随便命名系列手机";
        if (omsOrder != null) {
            orderSn = omsOrder.getOrderSn();
            totalAmount = omsOrder.getTotalAmount();
        }
        //制作请求支付宝的表单信息，提供支付宝需要的数据
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(orderSn);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject(subject);
        paymentService.savePaymentInfo(paymentInfo);
        paymentService.sendAlipayPaymentCheck(orderSn,5);//将用户引导至支付宝页面后，要主动发消息去检查支付状态
        subject = URLEncoder.encode(subject,"UTF-8");
        return "redirect:http://127.0.0.1:8087/fakeAlipayPage?out_trade_no="+orderSn+ "&total_amount=" + totalAmount.toString()+"&subject="+subject;
    }

    /**
     * 模拟支付宝
     * @param out_trade_no
     * @param subject
     * @param total_amount
     * @return
     */
    @RequestMapping("/fakeAlipayPage")
    public String fakeAlipayPage(String out_trade_no,String subject,String total_amount,ModelMap map) {
        map.put("out_trade_no",out_trade_no);
        map.put("subject",subject);
        map.put("total_amount",total_amount);
        return "fakeAlipay";
    }
    @RequestMapping("/fake/alipay/callback/return")
    @LoginRequired
    public String fakeAlipayCallBackReturn(HttpServletRequest request) {
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String total_amount = request.getParameter("total_amount");
        String call_back_content = request.getQueryString();//获得查询字符串，即get请求问号后的全部参数

        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if (StringUtils.isNotBlank(sign)) {
            // 验签成功
            // 更新用户的支付状态
            /* PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);// 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setTotalAmount(new BigDecimal(total_amount));*/
            paymentService.updatePayment(out_trade_no,trade_no,call_back_content,total_amount);
            //发送消息更新订单状态至 1待发货
            paymentService.sendPaymentSuccess(out_trade_no);
        }

        return "finish";
    }
}
