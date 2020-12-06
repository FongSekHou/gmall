package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;

import java.math.BigDecimal;
import java.util.List;

public interface SkuService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);
    PmsSkuInfo getSkuInfoByIdFromDb(String skuId);
    PmsSkuInfo getSkuInfoById(String skuId);
    PmsSkuInfo getSkuInfoByIdAndRedisson(String skuId);
    List<PmsSkuInfo> getSkuInfoByProductId(String productId);

    List<PmsSkuInfo> getSkuInfos();

    boolean checkPrice(String productSkuId, BigDecimal price);
}
