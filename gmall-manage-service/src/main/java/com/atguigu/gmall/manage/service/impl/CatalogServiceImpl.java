package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog1Mapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog2Mapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog3Mapper;
import com.atguigu.gmall.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class CatalogServiceImpl implements CatalogService {

    @Autowired
    PmsBaseCatalog1Mapper catalog1Mapper;
    @Autowired
    PmsBaseCatalog2Mapper catalog2Mapper;
    @Autowired
    PmsBaseCatalog3Mapper catalog3Mapper;


    @Override
    public List<PmsBaseCatalog1> getCatalog1() {
        return catalog1Mapper.selectAll();
    }

    @Override
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id) {

        Example example = new Example(PmsBaseCatalog2.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("catalog1Id",catalog1Id);
        return catalog2Mapper.selectByExample(example);
    }

    @Override
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id) {
        Example example = new Example(PmsBaseCatalog3.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("catalog2Id",catalog2Id);
        return catalog3Mapper.selectByExample(example);
    }
}
