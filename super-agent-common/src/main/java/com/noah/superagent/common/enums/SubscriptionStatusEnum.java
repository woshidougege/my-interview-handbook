package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
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
public enum SubscriptionStatusEnum implements BaseEnum<Integer> {

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

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static SubscriptionStatusEnum getByCode(Integer code) {
        return BaseEnum.getByCode(SubscriptionStatusEnum.class, code);
    }
}
