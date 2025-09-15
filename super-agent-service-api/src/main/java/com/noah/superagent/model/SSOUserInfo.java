package com.noah.superagent.model;

import lombok.Data;

/**
 * SSO认证中心用户信息
 * 用于接收从认证中心返回的用户信息
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
public class SSOUserInfo {

    /**
     * 认证中心用户ID（int类型）
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 用户状态（1-正常 0-禁用）
     */
    private Integer status;

    /**
     * 角色列表
     */
    private String roles;

    /**
     * 权限列表
     */
    private String permissions;

    /**
     * Token
     */
    private String token;

    /**
     * Token过期时间
     */
    private Long tokenExpire;

}
