package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
public enum WorkspaceStatusEnum {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 禁用
     */
    DISABLED(2, "禁用");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static WorkspaceStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WorkspaceStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
