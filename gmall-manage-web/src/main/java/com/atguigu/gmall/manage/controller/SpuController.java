package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@CrossOrigin
public class SpuController {

    @Reference
    SpuService spuService;

    @RequestMapping("/spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id){
        return spuService.getSpuByCatalog3Id(catalog3Id);
    }

    @RequestMapping("/saveSpuInfo")
    @ResponseBody
    public void saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){
        spuService.savePmsProductInfo(pmsProductInfo);
    }

    /**
     * 将图片存储至分布式文件存储系统并返回路径
     * @param multipartFile
     * @return
     */
    @RequestMapping("/fileUpload")
    @ResponseBody
    public String fileUpload(MultipartFile multipartFile){


        return "fake file path";
    }

    @RequestMapping("/baseSaleAttrList")
    @ResponseBody
    public List<PmsBaseSaleAttr> baseSaleAttrList(){
        return spuService.getAllBaseSaleAttr();
    }

    @RequestMapping("/spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){
        return spuService.getSpuSaleAttrBySpuId(spuId);
    }

    @RequestMapping("/spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(String spuId){
        return spuService.getSpuImageBySpuId(spuId);
    }
}
