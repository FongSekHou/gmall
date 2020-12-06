package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.constant.RedisConst;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;
    @Autowired
    RedisUtil redisUtil;
   /* @Autowired
    RedissonClient redissonClient;*/

    @Override
    public OmsCartItem getOmsCartItemFromDb(String memberId, String skuId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        if (omsCartItems.size() == 1) {
            return omsCartItems.get(0);
        } else if (omsCartItems.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Database query error, more than one item in shopping cart.");
        }
    }

    @Override
    public void saveOmsCartItem(OmsCartItem omsCartItem) {
        omsCartItemMapper.insert(omsCartItem);
    }

    @Override
    public void updateOmsCartItem(OmsCartItem omsCartItem) {
        omsCartItemMapper.updateByPrimaryKeySelective(omsCartItem);
    }

    @Override
    public void flushCartCache(String memberId) {
        /*RMap<String, OmsCartItem> map = redissonClient.getMap(RedisConst.getCartListKey(memberId));
        //先删除
        for(OmsCartItem omsCartItem:map.values()){
            map.remove(omsCartItem);
        }
        //再从数据库中查询，写入redis进行同步
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        for(OmsCartItem omsCartItem1:omsCartItems){
            map.put(omsCartItem1.getMemberId(),omsCartItem1);//map.put()已经往redis里写数据了
        }
        //redissonClient.shutdown();*/
        //根据memberid去数据库取出最新值
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        Map<String, String> map = new HashMap<>();
        //准备map集合作为key
        for (OmsCartItem cartItem : omsCartItems) {
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }
        Jedis jedis = redisUtil.getJedis();
        try {
            String key = RedisConst.getCartListKey(memberId);
            jedis.del(key);
            jedis.hmset(key, map);
        } catch (Exception e) {
            throw new RuntimeException("jedis flushCartCache exception.");
        } finally {
            jedis.close();
        }
    }

    @Override
    public List<OmsCartItem> getCartList(String memberId) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        try {
            String key = RedisConst.getCartListKey(memberId);
            List<String> list = jedis.hvals(key);//获得该hash的所有值
            if (list.size() > 0) {//redis中有值
                for (String str : list) {
                    OmsCartItem omsCartItem = JSON.parseObject(str, OmsCartItem.class);
                    omsCartItems.add(omsCartItem);
                }
            } else {
                OmsCartItem omsCartItem = new OmsCartItem();
                omsCartItem.setMemberId(memberId);
                omsCartItems = omsCartItemMapper.select(omsCartItem);
                if (omsCartItems.size() != 0) {
                    //从数据库取出数据后还要将数据写入redis中，并解决缓存击穿，缓存雪崩，缓存穿透的问题
                    Map<String, String> map = new HashMap<>();
                    for (OmsCartItem omsCartItem1 : omsCartItems) {
                        map.put(omsCartItem1.getMemberId(), JSON.toJSONString(omsCartItem1));//map.put()已经往redis里写数据了
                    }
                    //缓存雪崩：给上随机值作为过期时限
                    //缓存击穿：上分布式锁
                    //防止缓存穿透,设为空值并设置固定过期时限
                    jedis.hmset(key, map);
                }
            }
            return omsCartItems;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("jedis getCartList exception.");
        } finally {
            jedis.close();
        }
        /*RMap<String, OmsCartItem> map = redissonClient.getMap(RedisConst.getCartListKey(memberId));
        if (map.size()>0){//map中有数据证明redis中有数据
            for(OmsCartItem omsCartItem:map.values()){
                omsCartItems.add(omsCartItem);
            }
        }else {
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(memberId);
            omsCartItems = omsCartItemMapper.select(omsCartItem);
           if(omsCartItems.size()!=0){
               //从数据库取出数据后还要将数据写入redis中，并解决缓存击穿，缓存雪崩，缓存穿透的问题
               for(OmsCartItem omsCartItem1:omsCartItems){
                   map.put(omsCartItem1.getMemberId(),omsCartItem1);//map.put()已经往redis里写数据了
               }
               //缓存雪崩：给上随机值作为过期时限
               //缓存击穿：上分布式锁
           }else {//防止缓存穿透
               map.put("",null);
               map.expire(60, TimeUnit.SECONDS);
           }
        }
        //redissonClient.shutdown();*/
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        Example example = new Example(OmsCartItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,example);
        flushCartCache(omsCartItem.getMemberId());
    }

    @Override
    public void removeCartItems(List<String> selectedCartItemIds) {
        for (String id:selectedCartItemIds){
            omsCartItemMapper.deleteByPrimaryKey(id);
        }
    }
}
