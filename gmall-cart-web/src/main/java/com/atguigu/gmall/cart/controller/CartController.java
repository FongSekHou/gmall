package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @RequestMapping("/addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(Integer num, String skuId, HttpServletRequest request, HttpServletResponse response) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();//购物车列表

        PmsSkuInfo skuInfo = skuService.getSkuInfoById(skuId);
        OmsCartItem omsCartItem = new OmsCartItem();

        // 将商品信息封装成购物车信息
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("123456789");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(num));
        //omsCartItem.setIsChecked("1");//默认被选中
        // 判断用户是否登录
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
/*        String memberId = "1";
        String nickname = "windir";*/

        if (StringUtils.isBlank(memberId)) {// 用户没有登录
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartListCookie)) {
                omsCartItems.add(omsCartItem);
            } else {
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                boolean flag = false;
                for (OmsCartItem omsCartItem1 : omsCartItems) {
                    if (omsCartItem1.getProductSkuId().equals(skuId)) {
                        flag = true;
                        break;
                    }
                }
                if (flag) {//如果购物车中已存在该商品项
                    for (OmsCartItem omsCartItem1 : omsCartItems) {
                        if (omsCartItem1.getProductSkuId().equals(skuId)) {
                            omsCartItem1.setQuantity(omsCartItem1.getQuantity().add(omsCartItem.getQuantity()));//更新数量
                            omsCartItem1.setTotalPrice(omsCartItem1.getPrice().multiply(omsCartItem1.getQuantity()));//更新总价
                        }
                    }
                } else {
                    omsCartItems.add(omsCartItem);
                }
            }
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(omsCartItems), 60 * 60 * 72, true);
        } else {//用户已登录
            OmsCartItem omsCartItem1 = cartService.getOmsCartItemFromDb(memberId, skuId);
            if (omsCartItem1 == null) {
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname(nickname);
                cartService.saveOmsCartItem(omsCartItem);
            } else {
                omsCartItem1.setQuantity(omsCartItem1.getQuantity().add(omsCartItem.getQuantity()));
                omsCartItem1.setTotalPrice(omsCartItem1.getPrice().multiply(omsCartItem1.getQuantity()));//设置购物车总价
                cartService.updateOmsCartItem(omsCartItem1);
            }
            cartService.flushCartCache(memberId);
        }

        return "redirect:/success";
    }

    @RequestMapping("/success")
    public String success() {
        return "success";
    }

    @RequestMapping("/cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, ModelMap map) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String memberId = (String) request.getAttribute("memberId");
        /*String nickname = (String) request.getAttribute("nickname");*/
/*        String memberId = "1";
        String nickname = "windir";*/

        if (StringUtils.isBlank(memberId)) {//未登录
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        } else {//已登录
            omsCartItems = cartService.getCartList(memberId);
        }
        BigDecimal totalPrice = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            if ("1".equals(omsCartItem.getIsChecked())) {
                totalPrice = totalPrice.add(omsCartItem.getTotalPrice());
            }

        }

        map.put("cartList", omsCartItems);
        map.put("totalAmount", totalPrice);
        return "cartList";
    }

    @RequestMapping("/checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked, String skuId, HttpServletRequest request, ModelMap modelMap) {
        String memberId = (String) request.getAttribute("memberId");
        //String nickname = (String) request.getAttribute("nickname");
/*        String memberId = "1";
        String nickname = "windir";*/
        List<OmsCartItem> omsCartItems;
        if(StringUtils.isNotBlank(memberId)){//已登录
            // 调用服务，修改状态
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(memberId);
            omsCartItem.setProductSkuId(skuId);
            omsCartItem.setIsChecked(isChecked);
            cartService.checkCart(omsCartItem);
            // 将最新的数据从缓存中查出，渲染给内嵌页
            omsCartItems = cartService.getCartList(memberId);
        }else {//未登录
            omsCartItems = JSON.parseArray(CookieUtil.getCookieValue(request,"cartListCookie",true),OmsCartItem.class);
            if(omsCartItems!=null){
                for(OmsCartItem omsCartItem:omsCartItems){
                    if (omsCartItem.getProductSkuId().equals(skuId)){
                        omsCartItem.setIsChecked(isChecked);
                    }
                }
            }
        }

        BigDecimal totalAmount = new BigDecimal(0);
        // 被勾选商品的总额
        if(omsCartItems!=null){
            for (OmsCartItem omsCartItem1 : omsCartItems) {
                if ("1".equals(omsCartItem1.getIsChecked())){
                    totalAmount = totalAmount.add(omsCartItem1.getTotalPrice());
                }
            }
        }
        modelMap.put("totalAmount", totalAmount);
        modelMap.put("cartList", omsCartItems);
        return "cartListInner";
    }

}
