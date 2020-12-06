package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;

import java.util.List;
import java.util.Set;

public interface AttrService {

    List<PmsBaseAttrInfo> getAtteInfoByCatalog3Id(String catalog3Id);
    List<PmsBaseAttrValue> getAttrValueByAttrId(String attrId);
    void saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);
    List<PmsBaseAttrInfo> getAtteInfosByValueSet(Set<String> valueSet);
}
