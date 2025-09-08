package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum implements BaseEnum<Integer> {

    /**
     * 正常
     */
    ACTIVE(1, "正常"),

    /**
     * 禁用
     */
    DISABLED(0, "禁用");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static UserStatusEnum getByCode(Integer code) {
        return BaseEnum.getByCode(UserStatusEnum.class, code);
    }
}
