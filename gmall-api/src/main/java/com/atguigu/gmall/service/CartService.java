package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

public interface CartService {
    OmsCartItem getOmsCartItemFromDb(String memberId, String skuId);

    void saveOmsCartItem(OmsCartItem omsCartItem);

    void updateOmsCartItem(OmsCartItem omsCartItem);

    void flushCartCache(String memberId);

    List<OmsCartItem> getCartList(String memberId);

    void checkCart(OmsCartItem omsCartItem);

    void removeCartItems(List<String> selectedCartItemIds);
}
