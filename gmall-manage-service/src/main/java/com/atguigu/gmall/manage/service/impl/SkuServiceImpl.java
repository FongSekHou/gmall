package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.constant.RedisConst;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedissonClient redissonClient;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);

        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(pmsSkuInfo.getId());
            pmsSkuImage.setProductImgId(pmsSkuImage.getSpuImgId());
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }

        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(pmsSkuInfo.getId());
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(pmsSkuInfo.getId());
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
    }

    /**
     * 前往数据库查询sku信息的方法
     *
     * @param skuId
     * @return
     */
    @Override
    public PmsSkuInfo getSkuInfoByIdFromDb(String skuId) {
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(skuId);
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        if (pmsSkuInfo != null) {
            pmsSkuImage.setSkuId(pmsSkuInfo.getId());
            pmsSkuInfo.setSkuImageList(pmsSkuImageMapper.select(pmsSkuImage));
            PmsSkuSaleAttrValue pmsSkuSaleAttrValue = new PmsSkuSaleAttrValue();
            pmsSkuSaleAttrValue.setSkuId(pmsSkuInfo.getId());
            pmsSkuInfo.setSkuSaleAttrValueList(pmsSkuSaleAttrValueMapper.select(pmsSkuSaleAttrValue));
        }
        return pmsSkuInfo;
    }

    /**
     * 前往redis查询sku信息的方法
     * 要防止缓存击穿，雪崩，穿透
     * @param skuId
     * @return
     */
    @Override
    public PmsSkuInfo getSkuInfoById(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        String skuInfoKey = RedisConst.getSkuInfoKey(skuId);
        PmsSkuInfo skuInfo = null;
        try {
            String value = jedis.get(skuInfoKey);
            if (StringUtils.isBlank(value)) {//redis查不到，要去数据库，先上锁
                String lockKey = RedisConst.getSkuInfoLock(skuId);
                String token = UUID.randomUUID().toString();//token的锁的value，用于防止删掉别人的锁
                String flag = jedis.set(lockKey, token,"nx","px",10*1000);//上分布式锁，该方法相当于setnx与setex一起用,flag若为ok就是设置成功,10秒为失效时间
                if (StringUtils.isNotBlank(flag)&&flag.equals("OK")) {//设置成功，获得锁成功
                    skuInfo = getSkuInfoByIdFromDb(skuId);
                    if (skuInfo == null) {//如果该id去数据库查出来的数据仍为空，那也在redis中设上空字符串防止缓存穿透
                        jedis.setex(skuInfoKey, 60, "");
                    } else {
                        jedis.set(skuInfoKey, JSON.toJSONString(skuInfo));
                    }
                    //释放锁
                    String newToken = jedis.get(lockKey);
                    if(StringUtils.isNotBlank(newToken)&&token.equals(newToken)){//比较token，如果是自己的锁就删除
                        //jedis.del(lockKey);普通删除
                        jedis.eval(RedisConst.getLuaScriptToDelLock(),Collections.singletonList(lockKey),Collections.singletonList(token));//使用lua脚本迅速删除
                    }
                } else {//设置失败就进入else分支
                    //休眠2秒后再重新查询数据
                    Thread.sleep(1000 * 2);
                    return getSkuInfoById(skuId);//自选，重新执行方法
                }
            } else {
                skuInfo = JSON.parseObject(value, PmsSkuInfo.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            jedis.close();
        }
        return skuInfo;
    }

    /**
     *
     * @param skuId
     * @return
     */
    @Override
    public PmsSkuInfo getSkuInfoByIdAndRedisson(String skuId) {
        String skuKey = RedisConst.getSkuInfoKey(skuId);
        RBucket<PmsSkuInfo> bucket = redissonClient.getBucket(skuKey);
        PmsSkuInfo skuInfo = null;
        if(bucket.get()==null){
            RLock lock = redissonClient.getLock(RedisConst.getSkuInfoLock(skuId));
            lock.lock(10,TimeUnit.SECONDS);//上锁
            skuInfo  = getSkuInfoByIdFromDb(skuId);
            if(skuInfo==null){
                bucket.set(null,60, TimeUnit.SECONDS);
            }else {
                bucket.set(skuInfo);
            }
            lock.unlock();//解锁
        }else {
            skuInfo = bucket.get();
        }
        redissonClient.shutdown();//关闭redisson客户端
        return skuInfo;
    }

    /**
     * 根据spu的id获得spu系列下所有sku的销售属性，用于给前端拼接销售属性与sku对应的hash表
     *
     * @param productId
     * @return
     */
    @Override
    public List<PmsSkuInfo> getSkuInfoByProductId(String productId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setProductId(productId);
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.select(pmsSkuInfo);
        for (PmsSkuInfo pmsSkuInfo1 : pmsSkuInfos) {
            PmsSkuSaleAttrValue pmsSkuSaleAttrValue = new PmsSkuSaleAttrValue();
            pmsSkuSaleAttrValue.setSkuId(pmsSkuInfo1.getId());
            pmsSkuInfo1.setSkuSaleAttrValueList(pmsSkuSaleAttrValueMapper.select(pmsSkuSaleAttrValue));
        }
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getSkuInfos() {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(pmsSkuInfo.getId());
            List<PmsSkuAttrValue> list = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(list);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(productSkuId);
        if(price.compareTo(pmsSkuInfo.getPrice())==0){
            return true;
        }
        return false;
    }


}
