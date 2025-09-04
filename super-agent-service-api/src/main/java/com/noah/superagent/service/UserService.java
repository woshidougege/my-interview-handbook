package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.model.UserDO;

/**
 * 用户服务接口
 * 业务层操作DO对象，与前端DTO解耦
 *
 * @author System
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param userDO 用户领域对象
     * @return 创建后的用户信息
     */
    UserDO createUser(UserDO userDO);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserDO getUserById(Long id);

    /**
     * 分页查询用户
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param keyword 关键词
     * @return 分页结果
     */
    PageResponse<UserDO> getUserPage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 更新用户信息
     *
     * @param userDO 用户领域对象
     * @return 更新后的用户信息
     */
    UserDO updateUser(UserDO userDO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 检查手机号是否已存在
     *
     * @param phone 手机号
     * @return 是否存在
     */
    boolean isPhoneExists(String phone);
}
