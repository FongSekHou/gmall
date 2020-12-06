package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;

    @RequestMapping("/{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap map){
        PmsSkuInfo pmsSkuInfo = skuService.getSkuInfoById(skuId);
        if(pmsSkuInfo!=null){
            List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.getSpuSaleAttrBySku(pmsSkuInfo.getProductId(),skuId);
            List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuInfoByProductId(pmsSkuInfo.getProductId());
            Map skuSaleAttrHash = new HashMap();
            for(PmsSkuInfo pmsSkuInfo1:pmsSkuInfos){
                String value = pmsSkuInfo1.getId();
                List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo1.getSkuSaleAttrValueList();
                StringBuilder key = new StringBuilder();
                for(int i=0;i<skuSaleAttrValueList.size();i++){
                    key.append(skuSaleAttrValueList.get(i).getSaleAttrValueId());
                    if(i<skuSaleAttrValueList.size()-1){
                        key.append("|");
                    }
                }
                skuSaleAttrHash.put(key,value);
            }
            String skuSaleAttrHashJsonStr = JSON.toJSONString(skuSaleAttrHash);//hash表不放在request域中，用json字符串发给前端接收
            map.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);
            map.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);
        }
        map.put("skuInfo",pmsSkuInfo);
        return "item";
    }

}
