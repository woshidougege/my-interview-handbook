package com.noah.superagent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户领域对象
 * 
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDO extends BaseDO {

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
