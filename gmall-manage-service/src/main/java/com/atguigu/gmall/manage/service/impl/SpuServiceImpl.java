package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;
    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;
    @Autowired
    PmsProductImageMapper pmsProductImageMapper;
    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;
    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;

    @Override
    public List<PmsProductInfo> getSpuByCatalog3Id(String catalog3Id) {
        Example example = new Example(PmsProductInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("catalog3Id", catalog3Id);
        return pmsProductInfoMapper.selectByExample(example);
    }

    @Override
    public List<PmsBaseSaleAttr> getAllBaseSaleAttr() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    @Override
    public void savePmsProductInfo(PmsProductInfo pmsProductInfo) {
        pmsProductInfoMapper.insert(pmsProductInfo);
        List<PmsProductImage> spuImageList = pmsProductInfo.getSpuImageList();

        for (PmsProductImage pmsProductImage : spuImageList) {
            pmsProductImage.setProductId(pmsProductInfo.getId());
            pmsProductImageMapper.insert(pmsProductImage);
        }

        List<PmsProductSaleAttr> spuSaleAttrList = pmsProductInfo.getSpuSaleAttrList();
        for (PmsProductSaleAttr pmsProductSaleAttr : spuSaleAttrList) {
            pmsProductSaleAttr.setProductId(pmsProductInfo.getId());
            pmsProductSaleAttrMapper.insert(pmsProductSaleAttr);

            List<PmsProductSaleAttrValue> pmsProductSaleAttrValueList = pmsProductSaleAttr.getSpuSaleAttrValueList();
            for (PmsProductSaleAttrValue pmsProductSaleAttrValue : pmsProductSaleAttrValueList) {
                pmsProductSaleAttrValue.setProductId(pmsProductInfo.getId());
                pmsProductSaleAttrValueMapper.insert(pmsProductSaleAttrValue);
            }

        }

    }

    @Override
    public List<PmsProductSaleAttr> getSpuSaleAttrBySpuId(String spuId) {
        Example example = new Example(PmsProductSaleAttr.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("productId", spuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.selectByExample(example);
        /*example = new Example(PmsProductSaleAttrValue.class);
        criteria = example.createCriteria();*/
        for (PmsProductSaleAttr pmsProductSaleAttr : pmsProductSaleAttrs) {
            /* criteria.andEqualTo("productId",pmsProductSaleAttr.getProductId()).andEqualTo("saleAttrId",pmsProductSaleAttr.getSaleAttrId());*/
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setProductId(spuId);
            pmsProductSaleAttrValue.setSaleAttrId(pmsProductSaleAttr.getSaleAttrId());
            pmsProductSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue));
        }
        return pmsProductSaleAttrs;
    }

    @Override
    public List<PmsProductImage> getSpuImageBySpuId(String spuId) {
        Example example = new Example(PmsProductImage.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("productId", spuId);
        return pmsProductImageMapper.selectByExample(example);
    }

    /**
     * 查出当前sku所属spu的所有销售属性以及当前sku所拥有的销售属性
     *
     * @param productId
     * @param skuId
     * @return
     */
    @Override
    public List<PmsProductSaleAttr> getSpuSaleAttrBySku(String productId, String skuId) {
      /*  PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(productId);
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        for(PmsProductSaleAttr pmsProductSaleAttr1:pmsProductSaleAttrs){
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setSaleAttrId(pmsProductSaleAttr1.getSaleAttrId());
            pmsProductSaleAttrValue.setProductId(productId);
            pmsProductSaleAttr1.setSpuSaleAttrValueList(pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue));
        }
        return pmsProductSaleAttrs;*/
        return pmsProductSaleAttrMapper.selectSpuSaleAttrListCheckBySku(productId, skuId);
    }
}
