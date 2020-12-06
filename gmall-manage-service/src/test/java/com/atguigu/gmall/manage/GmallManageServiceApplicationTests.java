package com.atguigu.gmall.manage;

import com.atguigu.gmall.bean.PmsSkuInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = GmallManageServiceApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class GmallManageServiceApplicationTests {

    @Autowired
    RedissonClient redissonClient;

    /**
     * redisson常用方法
     * getBucket(key)、getList(key)、getSet(key)、getMap(key)、getSortedSet(key) 返回值是对象
     * RList、RMap、RSet、RSortedSet、RQueue、RDueue(Double Queue双端队列)
     * set(value,expiretime,timeunit)设置值和过期时间
     * set(value)设置值
     * tryset(value)相当于setnx
     * expire(expiretime,timeunit)设置过期时间
     * getLock（）
     * shutdown() 关闭redissonClient客户端
     */
    @Test
    public void testRedission(){


    }

}
