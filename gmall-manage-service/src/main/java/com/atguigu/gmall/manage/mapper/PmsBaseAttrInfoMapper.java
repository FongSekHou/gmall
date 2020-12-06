package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Set;

public interface PmsBaseAttrInfoMapper extends Mapper<PmsBaseAttrInfo> {
    //自己写的根据set里的id查询所有平台属性和平台属性值的方法
    List<PmsBaseAttrInfo> selectAtteInfosByValueSet(@Param("valueSet")Set<String> valueSet);
    //课件中根据set里的id查询所有平台属性和平台属性值的方法
    List<PmsBaseAttrInfo> selectAttrValueListByValueId(@Param("valueIdStr") String valueIdStr);
}
