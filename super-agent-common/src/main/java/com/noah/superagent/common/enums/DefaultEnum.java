package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 默认状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DefaultEnum implements BaseEnum<Integer> {

    /**
     * 非默认
     */
    NOT_DEFAULT(0, "非默认"),

    /**
     * 默认
     */
    DEFAULT(1, "默认");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static DefaultEnum getByCode(Integer code) {
        return BaseEnum.getByCode(DefaultEnum.class, code);
    }
}
