package com.noah.superagent.service;

import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.request.UserCreateRequest;
import com.noah.superagent.common.dto.request.UserUpdateRequest;
import com.noah.superagent.common.dto.response.UserResponse;

/**
 * 用户服务接口
 *
 * @author System
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param request 创建请求
     * @return 用户信息
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserResponse getUserById(Long id);

    /**
     * 分页查询用户
     *
     * @param request 分页请求
     * @return 分页结果
     */
    PageResponse<UserResponse> getUserPage(PageRequest request);

    /**
     * 更新用户信息
     *
     * @param id 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserResponse updateUser(Long id, UserUpdateRequest request);

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
