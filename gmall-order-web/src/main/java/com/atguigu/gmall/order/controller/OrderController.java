package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;
    @Reference
    CartService cartService;
    @Reference
    OrderService orderService;
    @Reference
    SkuService skuService;

    @RequestMapping("/toTrade")
    @LoginRequired
    public String toTrade (HttpServletRequest request, ModelMap map) {
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = userService.getReceiveAddressByMemberId(memberId);
        List<OmsCartItem> cartList = cartService.getCartList(memberId);

        BigDecimal totalAmount = new BigDecimal("0");//计算订单页商品的总价
        //将购物车数据项变为订单数据项
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        for (OmsCartItem omsCartItem : cartList) {
            if (omsCartItem.getIsChecked().equals("1")) {
                totalAmount = totalAmount.add(omsCartItem.getTotalPrice());//计算价格

                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItems.add(omsOrderItem);
            }
        }

        String tradeCode = orderService.generateTradeCode(memberId);//生成交易码并返回给前端

        map.put("nickname", nickname);
        map.put("userAddressList", umsMemberReceiveAddresses);
        map.put("omsOrderItems", omsOrderItems);
        map.put("totalAmount", totalAmount);
        map.put("tradeCode", tradeCode);

        return "trade";
    }

    @RequestMapping("/submitOrder")
    @LoginRequired
    public ModelAndView submitOrder (String receiveAddressId, String tradeCode, HttpServletRequest request, BigDecimal totalAmount) {
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        boolean success = orderService.checkTradeCode(tradeCode, memberId);
        if (success) {//如果交易码有效
            OmsOrder omsOrder = new OmsOrder();
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();

            //封装订单对象
            omsOrder.setAutoConfirmDay(7);//自动确认收货时间
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);//折扣金额
            //omsOrder.setFreightAmount(); 运费，支付后，在生成物流信息时生成
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("快点发货");//订单备注
            String outTradeNo = "gmall";
            outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo = outTradeNo + sdf.format(new Date());// 将时间字符串拼接到外部订单号

            omsOrder.setOrderSn(outTradeNo);//外部订单号
            omsOrder.setTotalAmount(totalAmount);//总价格
            omsOrder.setPayAmount(totalAmount);//实际支付金额，实际上可能有折扣或者优惠券之类降低价格的情况
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
            //封装收货地址信息
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            // 封装确认收货时间，当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, 1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);

            omsOrder.setSourceType(0);//订单来源：0->PC订单；1->app订单
            omsOrder.setStatus(0);//订单状态：0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单
            omsOrder.setOrderType(0);//订单类型：0->正常订单；1->秒杀订单


            /*根据用户id获得要购买的商品列表(购物车)，和总价格
              验价，验库存（不替用户做决定）（库存远程调用库存服务）
              根据用户信息查询当前用户的购物车中的商品数据
              循环将购物车中的商品对象封装成订单对象(订单详情)
             每次循环一个商品时，校验当前商品的库存和价格是否复合购买要求*/

            List<String> selectedCartItemIds = new ArrayList<>();//被选中的购物车项的id，订单信息生成后购物车项要被删除
            List<OmsCartItem> cartList = cartService.getCartList(memberId);
            for (OmsCartItem omsCartItem : cartList) {
                if (omsCartItem.getIsChecked().equals("1")) {
                    selectedCartItemIds.add(omsCartItem.getId());
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    // 检价
                    boolean flag = skuService.checkPrice(omsCartItem.getProductSkuId(), omsCartItem.getPrice());
                    if (!flag) {//验价不通过
                        ModelAndView mv = new ModelAndView("tradeFail");
                        return mv;
                    }
                    //用商品信息封装订单数据项对象
                    // 验库存,远程调用库存系统
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());//商品默认图片
                    omsOrderItem.setProductName(omsCartItem.getProductName());

                    omsOrderItem.setOrderSn(outTradeNo);// 外部订单号，用来和其他系统进行交互，防止重复
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());//商品分类id
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());//商品单价
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice().toString());//商品总价价
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity().intValue());//商品数量
                    omsOrderItem.setProductSkuCode("111111111111");//商品条形码
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());//skuid
                    omsOrderItem.setProductId(omsCartItem.getProductId());//spuid
                    omsOrderItem.setProductSn("仓库对应的商品编号");// 在仓库中的skuId

                    omsOrderItems.add(omsOrderItem);

                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);//将购物项对象封装进订单对象
            // 将订单和订单详情写入数据库，删除购物车的对应商品
            orderService.saveOrder(omsOrder);
            //拿到被勾选的全部购物车项的id，删除
            //cartService.removeCartItems(selectedCartItemIds);
            // 重定向到支付系统
            ModelAndView mv = new ModelAndView("redirect:http://127.0.0.1:8087/index");
            //重定向到支付系统
            return mv;
        } else {//交易码检验失败，提交订单失败
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }
    }

}
