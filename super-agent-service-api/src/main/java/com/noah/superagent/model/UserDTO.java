package com.noah.superagent.model;

import com.noah.superagent.common.enums.UserStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户领域对象
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends BaseDTO {

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
    private UserStatusEnum status;

}
