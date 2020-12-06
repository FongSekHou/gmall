package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> getSpuByCatalog3Id(String catalog3Id);
    List<PmsBaseSaleAttr> getAllBaseSaleAttr();
    void savePmsProductInfo(PmsProductInfo pmsProductInfo);
    List<PmsProductSaleAttr> getSpuSaleAttrBySpuId(String spuId);
    List<PmsProductImage> getSpuImageBySpuId(String spuId);

    List<PmsProductSaleAttr> getSpuSaleAttrBySku(String productId,String skuId);
}
