package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;
    @Reference
    AttrService attrService;

    @RequestMapping("/list.html")
    public String index(PmsSearchParam pmsSearchParam, ModelMap map){
        List<PmsSkuSearchInfo> list = searchService.list(pmsSearchParam);
		 map.put("skuLsInfoList",list);
		 
        Set<String> valueSet = new HashSet<String>();
        for (PmsSkuSearchInfo pmsSkuSearchInfo : list) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSkuSearchInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                valueSet.add(pmsSkuAttrValue.getValueId());
            }
        }
        List<PmsBaseAttrInfo> attrInfos =  attrService.getAtteInfosByValueSet(valueSet);
		
		
        StringBuilder urlParam = new StringBuilder();
        if(StringUtils.isNotBlank(pmsSearchParam.getKeyword())){
            urlParam.append("keyword="+pmsSearchParam.getKeyword());
            map.put("keyword",pmsSearchParam.getKeyword());
        }else if (StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())){
            urlParam.append("catalog3Id="+pmsSearchParam.getCatalog3Id());
        }
		
        String[] valueId = pmsSearchParam.getValueId();
        List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
		
        if(valueId!=null){
            for (String valueid : valueId) {
                urlParam.append("&valueId="+valueid);
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();//有一个参数就证明有一个面包屑
                pmsSearchCrumb.setValueId(valueid);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam,valueid));
                Iterator<PmsBaseAttrInfo> iterator = attrInfos.iterator();
                while (iterator.hasNext()){
                    PmsBaseAttrInfo attrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = attrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        if(pmsBaseAttrValue.getId().equals(valueid)){
                           pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);

            }
        }
        map.put("attrValueSelectedList",pmsSearchCrumbs);
        map.put("urlParam",urlParam.toString());
        map.put("attrList",attrInfos);
       
        return "list";
    }

    @RequestMapping("/index")
    @LoginRequired(loginSuccess = false)
    public String index(){
        return "index";
    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String... valueid) {
        StringBuilder urlParam = new StringBuilder();
        if(StringUtils.isNotBlank(pmsSearchParam.getKeyword())){
            urlParam.append("keyword="+pmsSearchParam.getKeyword());
        }else if (StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())){
            urlParam.append("catalog3Id="+pmsSearchParam.getCatalog3Id());
        }
        String[] valueId = pmsSearchParam.getValueId();
        for (String value : valueId) {
            if(!value.equals(valueid[0])){
                urlParam.append("&valueId="+value);
            }
        }
        return urlParam.toString();
    }


}
