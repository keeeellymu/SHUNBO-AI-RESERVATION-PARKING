package com.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.parking.dao.UserMapper;
import com.parking.model.dto.UserDTO;
import com.parking.model.dto.UserLoginRequestDTO;
import com.parking.model.dto.UserLoginResponseDTO;
import com.parking.model.entity.UserEntity;
import com.parking.service.UserService;
import com.parking.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("${wechat.appid}")
    private String appId;
    
    @Value("${wechat.secret}")
    private String appSecret;
    
    @Override
    public UserLoginResponseDTO login(UserLoginRequestDTO loginRequest) {
        UserEntity userEntity;
        boolean isNewUser = false;
        
        // 1. 检查是否是用户名密码登录
        if (loginRequest.getUsername() != null && loginRequest.getPassword() != null) {
            // 完全不依赖数据库的测试实现
            // 只要提供了用户名密码就认为登录成功
            userEntity = new UserEntity();
            userEntity.setId(1L); // 固定用户ID
            userEntity.setNickname(loginRequest.getUsername());
            userEntity.setCreateTime(LocalDateTime.now());
            userEntity.setUpdateTime(LocalDateTime.now());
            userEntity.setStatus(0); // 正常状态
            isNewUser = true;
        } 
        // 2. 微信小程序登录
        else if (loginRequest.getCode() != null) {
            // 简化处理，不调用实际的微信接口和数据库
            userEntity = new UserEntity();
            userEntity.setId(2L);
            userEntity.setOpenid("mock_openid_" + loginRequest.getCode());
            userEntity.setNickname("微信用户");
            userEntity.setCreateTime(LocalDateTime.now());
            userEntity.setUpdateTime(LocalDateTime.now());
            userEntity.setStatus(0);
        }
        // 3. 无效的登录请求
        else {
            throw new RuntimeException("登录参数无效，请提供用户名密码或微信code");
        }
        
        // 4. 生成token
        String token = generateToken(userEntity);
        
        // 5. 构建响应
        UserLoginResponseDTO response = new UserLoginResponseDTO();
        response.setToken(token);
        response.setIsNewUser(isNewUser);
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userEntity, userDTO);
        response.setUser(userDTO);
        
        return response;
    }
    
    @Override
    public UserEntity getUserByOpenid(String openid) {
        return userMapper.selectByOpenid(openid);
    }
    
    @Override
    @Transactional
    public UserDTO updateUserInfo(UserDTO userDTO) {
        System.out.println("========== 更新用户信息 ==========");
        System.out.println("接收到的用户DTO: " + userDTO);
        System.out.println("用户ID: " + userDTO.getId());
        System.out.println("车牌号: " + userDTO.getLicensePlate());
        System.out.println("电话: " + userDTO.getPhone());
        System.out.println("昵称: " + userDTO.getNickname());
        
        UserEntity userEntity = userMapper.selectById(userDTO.getId());
        if (userEntity == null) {
            throw new RuntimeException("用户不存在");
        }
        
        System.out.println("更新前的用户实体 - 车牌号: " + userEntity.getLicensePlate());
        System.out.println("更新前的用户实体 - 电话: " + userEntity.getPhone());
        
        // 只更新非空字段，避免覆盖现有数据
        if (userDTO.getNickname() != null && !userDTO.getNickname().trim().isEmpty()) {
            userEntity.setNickname(userDTO.getNickname().trim());
        }
        if (userDTO.getAvatarUrl() != null) {
            userEntity.setAvatarUrl(userDTO.getAvatarUrl());
        }
        if (userDTO.getPhone() != null && !userDTO.getPhone().trim().isEmpty()) {
            userEntity.setPhone(userDTO.getPhone().trim());
        }
        if (userDTO.getLicensePlate() != null && !userDTO.getLicensePlate().trim().isEmpty()) {
            userEntity.setLicensePlate(userDTO.getLicensePlate().trim());
            System.out.println("设置车牌号为: " + userDTO.getLicensePlate().trim());
        }
        
        userEntity.setUpdateTime(LocalDateTime.now());
        int updateResult = userMapper.updateById(userEntity);
        System.out.println("数据库更新结果: " + updateResult);
        
        // 再次查询确认更新结果
        UserEntity updatedEntity = userMapper.selectById(userDTO.getId());
        System.out.println("更新后的用户实体 - 车牌号: " + updatedEntity.getLicensePlate());
        System.out.println("更新后的用户实体 - 电话: " + updatedEntity.getPhone());
        
        // 返回更新后的用户信息
        UserDTO result = new UserDTO();
        BeanUtils.copyProperties(updatedEntity, result);
        System.out.println("返回的用户DTO - 车牌号: " + result.getLicensePlate());
        System.out.println("========== 更新用户信息完成 ==========");
        return result;
    }
    
    @Override
    public UserDTO getUserById(Long id) {
        UserEntity userEntity = userMapper.selectById(id);
        if (userEntity == null) {
            throw new RuntimeException("用户不存在");
        }
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userEntity, userDTO);
        return userDTO;
    }
    
    @Override
    public UserLoginResponseDTO refreshToken(Long userId) {
        // 1. 根据用户ID查询用户信息
        UserEntity userEntity = userMapper.selectById(userId);
        if (userEntity == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 生成新的token
        String token = generateToken(userEntity);
        
        // 3. 构建响应
        UserLoginResponseDTO response = new UserLoginResponseDTO();
        response.setToken(token);
        response.setIsNewUser(false); // 刷新token不属于新用户
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userEntity, userDTO);
        response.setUser(userDTO);
        
        return response;
    }
    
    /**
     * 从微信获取openid
     */
    private String getOpenidFromWechat(String code) {
        // 此处简化处理，实际应调用微信接口
        // String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + appSecret + "&js_code=" + code + "&grant_type=authorization_code";
        // 调用HTTP请求获取openid
        return "mock_openid_" + code; // 模拟openid
    }
    
    /**
     * 创建新用户
     */
    private UserEntity createNewUser(String openid) {
        UserEntity userEntity = new UserEntity();
        userEntity.setOpenid(openid);
        userEntity.setNickname("新用户");
        userEntity.setStatus(0); // 正常状态
        userEntity.setCreateTime(LocalDateTime.now());
        userEntity.setUpdateTime(LocalDateTime.now());
        return userEntity;
    }
    
    /**
     * 生成JWT token
     */
    private String generateToken(UserEntity userEntity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userEntity.getId());
        claims.put("openid", userEntity.getOpenid());
        return jwtUtil.createToken(claims);
    }
}