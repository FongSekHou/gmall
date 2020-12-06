package com.atguigu.gmall.constant;

public class RedisConst {

    /**
     * 查询用的redis value类型为string即可
     * 主键存储策略为sku：id：info
     * 正规一点可以是table_name:primary_key:attr_name(key):attr_value(value)
     * @param id
     * @return
     */
    public static final String getSkuInfoKey(String id){
        return "sku:"+id+":info";
    }

    /**
     * 获取写sku时的一把锁，用于解决缓存击穿问题
     * @param skuId
     * @return
     */
    public static final String getSkuInfoLock(String skuId){
        return "sku:"+skuId+":lock";
    }

    public static final String getLuaScriptToDelLock(){
        return "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    }

    /**
     * 购物车的主键存储策略为user：memberid：cart
     * value类型为hash
     * @param memberId
     * @return
     */
    public static final String getCartListKey(String memberId){
        return "user:"+memberId+":cart";
    }

    /**
     * 用户登录令牌的主键存储策列为user：memberid：token
     * @param memberId
     * @return
     */
    public static final String getUserTokenKey(String memberId){
        return "user:"+memberId+":token";
    }

    /**
     *用户信息的主键存储策略为user：username+password+info
     */
    public static final String getUserInfoKey(String username,String password){
        return "user:"+username+password+":info";
    }

    /**
     * 交易码的主键存储策略：user:memberId:tradeCode
     * 限时5min
     * @param memberId
     * @return
     */
    public static final String getTradeCodeKey(String memberId){
        return "user:"+memberId+":tradeCode";
    }
}
