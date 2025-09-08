package com.noah.superagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SubscriptionStatusEnum {

    /**
     * 生效中
     */
    ACTIVE(1, "生效中"),

    /**
     * 已过期
     */
    EXPIRED(2, "已过期"),

    /**
     * 已取消
     */
    CANCELLED(3, "已取消");

    private final Integer code;
    private final String desc;

    public static SubscriptionStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SubscriptionStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
