package com.noah.superagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author System
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    /**
     * 正常
     */
    ACTIVE(1, "正常"),

    /**
     * 禁用
     */
    DISABLED(0, "禁用");

    private final Integer code;
    private final String desc;

    public static UserStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
