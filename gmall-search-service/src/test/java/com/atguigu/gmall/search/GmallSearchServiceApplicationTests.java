package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSearchInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = GmallSearchServiceApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class GmallSearchServiceApplicationTests {

    @Autowired
    JestClient jestClient;
    @Reference
    SkuService skuService;

    @Test
    public void moveDataToEsFromMysql() throws IOException, InvocationTargetException, IllegalAccessException {
        List<PmsSkuInfo> skuInfos = skuService.getSkuInfos();
        List<PmsSkuSearchInfo> pmsSkuSearchInfos = new ArrayList<PmsSkuSearchInfo>();
        for (PmsSkuInfo skuInfo : skuInfos) {
            PmsSkuSearchInfo pmsSkuSearchInfo = new PmsSkuSearchInfo();
            BeanUtils.copyProperties(pmsSkuSearchInfo, skuInfo);
            pmsSkuSearchInfos.add(pmsSkuSearchInfo);
        }
        for (PmsSkuSearchInfo pmsSkuSearchInfo : pmsSkuSearchInfos) {
            Index put = new Index.Builder(pmsSkuSearchInfo).index("gmall").type("PmsSkuInfo").id(pmsSkuSearchInfo.getId() + "").build();
            jestClient.execute(put);
        }
    }

}
