package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.dao.entity.UserEntity;
import com.noah.superagent.convert.UserConvert;
import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.UserCreateRequest;
import com.noah.superagent.common.dto.request.UserUpdateRequest;
import com.noah.superagent.common.dto.response.UserResponse;
import com.noah.superagent.common.enums.UserStatusEnum;

import com.noah.superagent.dao.mapper.UserMapper;
import com.noah.superagent.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserConvert userConvert;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserCreateRequest request) {
        log.info("开始创建用户，手机号: {}", request.getPhone());
        
        // 检查手机号是否已存在
        if (isPhoneExists(request.getPhone())) {
            throw new RuntimeException("手机号已存在: " + request.getPhone());
        }
        
        // 转换为实体
        UserEntity userEntity = userConvert.toEntity(request);
        userEntity.setStatus(UserStatusEnum.ACTIVE.getCode());
        // ID由MyBatis Flex的雪花算法自动生成
        
        // TODO: 密码加密处理
        // userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // 保存用户
        int result = userMapper.insertSelective(userEntity);
        if (result <= 0) {
            throw new RuntimeException("用户创建失败");
        }
        
        log.info("用户创建成功，ID: {}", userEntity.getId());
        return userConvert.toResponse(userEntity);
    }

    @Override
    public UserResponse getUserById(Long id) {
        log.info("查询用户信息，ID: {}", id);
        
        UserEntity userEntity = userMapper.selectOneById(id);
        if (userEntity == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        return userConvert.toResponse(userEntity);
    }

    @Override
    public PageResponse<UserResponse> getUserPage(PageRequest request) {
        log.info("分页查询用户，页码: {}, 每页数量: {}, 关键词: {}", 
                request.getPageNum(), request.getPageSize(), request.getKeyword());
        
        // 创建分页对象
        Page<UserEntity> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        // 执行分页查询
        Page<UserEntity> userPage = userMapper.selectUserPage(page, request.getKeyword());
        
        // 转换结果
        return new PageResponse<>(
                userConvert.toResponseList(userPage.getRecords()),
                userPage.getTotalRow(),
                request.getPageNum(),
                request.getPageSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("更新用户信息，ID: {}", id);
        
        // 查询用户是否存在
        UserEntity existingUserEntity = userMapper.selectOneById(id);
        if (existingUserEntity == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        // 更新字段
        if (StringUtils.hasText(request.getNickname())) {
            existingUserEntity.setNickname(request.getNickname());
        }
        
        if (StringUtils.hasText(request.getPassword())) {
            // TODO: 密码加密处理
            existingUserEntity.setPassword(request.getPassword());
        }
        
        if (request.getStatus() != null) {
            existingUserEntity.setStatus(request.getStatus());
        }
        
        // 执行更新
        int result = userMapper.update(existingUserEntity);
        if (result <= 0) {
            throw new RuntimeException("用户更新失败");
        }
        
        log.info("用户更新成功，ID: {}", id);
        return userConvert.toResponse(existingUserEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        log.info("删除用户，ID: {}", id);
        
        // 检查用户是否存在
        UserEntity userEntity = userMapper.selectOneById(id);
        if (userEntity == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        // 执行删除
        int result = userMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("用户删除失败");
        }
        
        log.info("用户删除成功，ID: {}", id);
    }

    @Override
    public boolean isPhoneExists(String phone) {
        UserEntity userEntity = userMapper.selectByPhone(phone);
        return userEntity != null;
    }
}
