package com.noah.superagent.user.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.dao.entity.User;
import com.noah.superagent.user.convert.UserConvert;
import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.user.UserCreateRequest;
import com.noah.superagent.common.dto.user.UserUpdateRequest;
import com.noah.superagent.common.dto.user.UserResponse;
import com.noah.superagent.common.enums.UserStatusEnum;

import com.noah.superagent.dao.mapper.UserMapper;
import com.noah.superagent.user.service.UserService;
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
        User user = userConvert.toEntity(request);
        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        // ID由MyBatis Flex的雪花算法自动生成
        
        // TODO: 密码加密处理
        // user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // 保存用户
        int result = userMapper.insertSelective(user);
        if (result <= 0) {
            throw new RuntimeException("用户创建失败");
        }
        
        log.info("用户创建成功，ID: {}", user.getId());
        return userConvert.toResponse(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        log.info("查询用户信息，ID: {}", id);
        
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        return userConvert.toResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getUserPage(PageRequest request) {
        log.info("分页查询用户，页码: {}, 每页数量: {}, 关键词: {}", 
                request.getPageNum(), request.getPageSize(), request.getKeyword());
        
        // 创建分页对象
        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        // 执行分页查询
        Page<User> userPage = userMapper.selectUserPage(page, request.getKeyword());
        
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
        User existingUser = userMapper.selectOneById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        // 更新字段
        if (StringUtils.hasText(request.getNickname())) {
            existingUser.setNickname(request.getNickname());
        }
        
        if (StringUtils.hasText(request.getPassword())) {
            // TODO: 密码加密处理
            existingUser.setPassword(request.getPassword());
        }
        
        if (request.getStatus() != null) {
            existingUser.setStatus(request.getStatus());
        }
        
        // 执行更新
        int result = userMapper.update(existingUser);
        if (result <= 0) {
            throw new RuntimeException("用户更新失败");
        }
        
        log.info("用户更新成功，ID: {}", id);
        return userConvert.toResponse(existingUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        log.info("删除用户，ID: {}", id);
        
        // 检查用户是否存在
        User user = userMapper.selectOneById(id);
        if (user == null) {
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
        User user = userMapper.selectByPhone(phone);
        return user != null;
    }
}
