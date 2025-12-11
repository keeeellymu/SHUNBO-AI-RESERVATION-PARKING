package com.parking.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.model.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    
    /**
     * 根据openid查询用户
     * @param openid 微信openid
     * @return 用户实体
     */
    UserEntity selectByOpenid(String openid);
    
}