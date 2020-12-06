package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;
    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;

    @Override
    public List<PmsBaseAttrInfo> getAtteInfoByCatalog3Id(String catalog3Id) {
        Example example = new Example(PmsBaseAttrInfo.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("catalog3Id",catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.selectByExample(example);
        /*example = new Example(PmsBaseAttrValue.class);
        criteria = example.createCriteria();*/
        for(PmsBaseAttrInfo pmsBaseAttrInfo:pmsBaseAttrInfos){
           /*criteria.andEqualTo("attrId",pmsBaseAttrInfo.getId());*/
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
           pmsBaseAttrInfo.setAttrValueList(pmsBaseAttrValueMapper.select(pmsBaseAttrValue));
        }
        return pmsBaseAttrInfos;
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueByAttrId(String attrId) {
        Example example = new Example(PmsBaseAttrValue.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("attrId",attrId);
        return pmsBaseAttrValueMapper.selectByExample(example);
    }

    @Override
    public void saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        if(pmsBaseAttrInfo.getId()==null) {
            pmsBaseAttrInfoMapper.insert(pmsBaseAttrInfo);//先插入才能获取主键
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                pmsBaseAttrValueMapper.insert(pmsBaseAttrValue);
            }
        }else {
            Example example = new Example(PmsBaseAttrValue.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("attrId",pmsBaseAttrInfo.getId());
            List<PmsBaseAttrValue> delPmsBaseAttrValues = pmsBaseAttrValueMapper.selectByExample(example);
            for(PmsBaseAttrValue pmsBaseAttrValue:delPmsBaseAttrValues){
                pmsBaseAttrValueMapper.delete(pmsBaseAttrValue);
            }

            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for(PmsBaseAttrValue pmsBaseAttrValue:attrValueList){
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                pmsBaseAttrValueMapper.insert(pmsBaseAttrValue);
            }

        }
    }

    @Override
    public List<PmsBaseAttrInfo> getAtteInfosByValueSet(Set<String> valueSet) {
        //return pmsBaseAttrInfoMapper.selectAtteInfosByValueSet(valueSet);
        String valueIdStr = StringUtils.join(valueSet, ",");//41,45,46
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.selectAttrValueListByValueId(valueIdStr);
        return pmsBaseAttrInfos;
    }

}
