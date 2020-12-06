package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSkuSearchInfo;

import java.util.List;

public interface SearchService {

    List<PmsSkuSearchInfo> list(PmsSearchParam pmsSearchParam);
}
