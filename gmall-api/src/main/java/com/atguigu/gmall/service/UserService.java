package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {

    List<UmsMember> getAllMembers();

    UmsMember saveMember(UmsMember umsMember);

    void deleteMember(String id);

    void updateMember(UmsMember umsMember);

    UmsMember getUserForLogin(String username,String password);

    void saveUserToken(String token, String memberId);

    UmsMember getUserForSocialLogin(String uid, Integer weiboSourceType);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);


}
