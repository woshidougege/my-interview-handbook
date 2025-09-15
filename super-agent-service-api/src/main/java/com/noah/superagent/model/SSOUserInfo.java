package com.noah.superagent.model;

import lombok.Data;

/**
 * SSO认证中心用户信息
 * 字段名与SSO SDK的UserDto保持一致，便于MapStruct转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
public class SSOUserInfo {

    /**
     * 认证中心用户ID（与SSO保持一致为String类型）
     */
    private String userId;

    /**
     * 用户名（与SSO字段名保持一致）
     */
    private String userName;

    /**
     * 用户昵称（与SSO字段名保持一致）
     */
    private String nickName;

    /**
     * 手机号（与SSO字段名保持一致）
     */
    private String phonenumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 组织机构code
     */
    private String orgCode;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 租户code码
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 用户状态（与SSO保持一致为String类型）
     */
    private String status;

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
