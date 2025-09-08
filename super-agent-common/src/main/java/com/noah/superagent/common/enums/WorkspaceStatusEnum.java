package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作空间状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum WorkspaceStatusEnum implements BaseEnum<Integer> {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 禁用
     */
    DISABLED(2, "禁用");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static WorkspaceStatusEnum getByCode(Integer code) {
        return BaseEnum.getByCode(WorkspaceStatusEnum.class, code);
    }
}
