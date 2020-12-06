package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.constant.RedisConst;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> getAllMembers() {
        return userMapper.selectAll();
    }

    @Override
    public UmsMember saveMember(UmsMember umsMember) {
        try {
            userMapper.insertSelective(umsMember);
            return umsMember;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void deleteMember(String id) {
        try {
            userMapper.deleteByPrimaryKey(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMember(UmsMember umsMember) {
        try {
            userMapper.updateByPrimaryKeySelective(umsMember);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 登录时先查redis，再查数据库
     * @param username
     * @param password
     * @return
     */
    @Override
    public UmsMember getUserForLogin(String username, String password) {
        Jedis jedis = redisUtil.getJedis();
        UmsMember umsMember = null;
        try {
            String userStr = jedis.get(RedisConst.getUserInfoKey(username, password));
            if (StringUtils.isNotBlank(userStr)) {
                umsMember = JSON.parseObject(userStr, UmsMember.class);
            }else {
                //redis中无数据，前往mysql中查
                umsMember = new UmsMember();
                if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                    umsMember.setUsername(username);
                    umsMember.setPassword(password);
                }
                umsMember = userMapper.selectOne(umsMember);
                if(umsMember!=null){
                    jedis.setex("user:" + umsMember.getPassword()+umsMember.getUsername() + ":info",60*60*24, JSON.toJSONString(umsMember));//用户信息的过期时间是一天
                }else {
                    jedis.setex("user:" + umsMember.getPassword()+umsMember.getUsername() + ":info",60*60*24, "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return umsMember;
    }

    @Override
    public void saveUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        try {
            jedis.setex(RedisConst.getUserTokenKey(memberId), 60 * 5, token);//5分钟过期
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    /**
     * 该方法是查询第三方账户是否登录过当前系统，如果社交登录频率高，可以将信息放入缓存查询，根据user:uid:sourceType存储信息
     * @param uid
     * @param weiboSourceType
     * @return
     */
    @Override
    public UmsMember getUserForSocialLogin(String uid, Integer weiboSourceType) {
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceUid(uid);
        umsMember.setSourceType(weiboSourceType);
        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if(umsMembers.size()==0){
            return null;
        }else if(umsMembers.size()==1){
            return umsMembers.get(0);
        }else {
            throw new RuntimeException("more than one third-party user.");
        }
    }

    /**
     * 根据收货地址id获得收货地址信息
     * @param receiveAddressId
     * @return
     */
    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress1 = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
        return umsMemberReceiveAddress1;
    }


    /**
     * 根据用户id获得该用户的所有收货地址信息
     * @param memberId
     * @return
     */
    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        // 封装的参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);


//       Example example = new Example(UmsMemberReceiveAddress.class);
//       example.createCriteria().andEqualTo("memberId",memberId);
//       List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(example);

        return umsMemberReceiveAddresses;
    }
}
