package com.noah.superagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 启用/禁用状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum EnabledEnum {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 启用
     */
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;

    public static EnabledEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (EnabledEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
