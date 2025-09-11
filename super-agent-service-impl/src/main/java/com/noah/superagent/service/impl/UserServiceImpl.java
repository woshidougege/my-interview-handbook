package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.noah.superagent.dao.entity.UserEntity;
import com.noah.superagent.convert.UserPersistenceConvert;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.enums.UserStatusEnum;
import com.noah.superagent.dao.mapper.UserMapper;
import com.noah.superagent.model.UserDTO;
import com.noah.superagent.service.UserService;
import com.noah.superagent.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper,UserEntity> implements UserService  {

    private final UserMapper userMapper;
    private final UserPersistenceConvert userPersistenceConvert;
    private final UserCreditService userCreditService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO createUser(UserDTO userDO) {
        log.info("开始创建用户，手机号: {}", userDO.getPhone());
        
        // 检查手机号是否已存在
        if (isPhoneExists(userDO.getPhone())) {
            throw new RuntimeException("手机号已存在: " + userDO.getPhone());
        }
        
        // DTO -> Entity
        UserEntity userEntity = userPersistenceConvert.toEntity(userDO);
        userEntity.setStatus(UserStatusEnum.ACTIVE);
        // ID由MyBatis Flex的雪花算法自动生成
        
        // TODO: 密码加密处理
        // userEntity.setPassword(passwordEncoder.encode(userDO.getPassword()));
        
        // 保存用户
        int result = userMapper.insertSelective(userEntity);
        if (result <= 0) {
            throw new RuntimeException("用户创建失败");
        }
        
        log.info("用户创建成功，ID: {}", userEntity.getId());
        
        // 为新用户初始化免费套餐积分账户
        try {
            userCreditService.initFreePlanForUser(userEntity.getId());
            log.info("用户免费套餐积分账户初始化成功 - userId: {}", userEntity.getId());
        } catch (Exception e) {
            log.error("用户免费套餐积分账户初始化失败 - userId: {}, 错误: {}", 
                    userEntity.getId(), e.getMessage(), e);
            // 不抛出异常，避免影响用户注册流程
        }
        
        // Entity -> DTO
        return userPersistenceConvert.fromEntity(userEntity);
    }

    @Override
    public UserDTO getUserById(Long id) {
        log.info("查询用户信息，ID: {}", id);
        
        UserEntity userEntity = userMapper.selectOneById(id);
        if (userEntity == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        // Entity -> DTO
        return userPersistenceConvert.fromEntity(userEntity);
    }

    @Override
    public PageResponse<UserDTO> getUserPage(Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询用户，页码: {}, 每页数量: {}, 关键词: {}", 
                pageNum, pageSize, keyword);
        
        // 创建分页对象
        Page<UserEntity> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        Page<UserEntity> userPage = userMapper.selectUserPage(page, keyword);
        
        // 转换结果
        return new PageResponse<>(
                userPersistenceConvert.fromEntityList(userPage.getRecords()),
                userPage.getTotalRow(),
                pageNum,
                pageSize
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateUser(UserDTO userDO) {
        log.info("更新用户信息，ID: {}", userDO.getId());
        
        // 查询用户是否存在
        UserEntity existingUserEntity = userMapper.selectOneById(userDO.getId());
        if (existingUserEntity == null) {
            throw new RuntimeException("用户不存在: " + userDO.getId());
        }
        
        // 更新字段
        if (StringUtils.hasText(userDO.getUsername())) {
            existingUserEntity.setUsername(userDO.getUsername());
        }
        
        if (StringUtils.hasText(userDO.getPhone())) {
            existingUserEntity.setPhone(userDO.getPhone());
        }
        
        if (StringUtils.hasText(userDO.getPassword())) {
            existingUserEntity.setPassword(userDO.getPassword());
        }
        
        if (userDO.getStatus() != null) {
            existingUserEntity.setStatus(userDO.getStatus());
        }
        
        // 执行更新
        int result = userMapper.update(existingUserEntity);
        if (result <= 0) {
            throw new RuntimeException("用户更新失败");
        }
        
        log.info("用户更新成功，ID: {}", userDO.getId());
        // Entity -> DTO
        return userPersistenceConvert.fromEntity(existingUserEntity);
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
