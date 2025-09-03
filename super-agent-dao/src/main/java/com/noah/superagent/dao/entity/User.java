package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表 - 简化版
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user")
public class User extends BaseEntity {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * 用户状态 1-正常 0-禁用
     */
    private Integer status;
}
