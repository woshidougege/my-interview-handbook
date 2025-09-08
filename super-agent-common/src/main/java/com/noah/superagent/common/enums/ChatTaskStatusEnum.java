package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对话任务状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ChatTaskStatusEnum {

    /**
     * 进行中
     */
    IN_PROGRESS(1, "进行中"),

    /**
     * 已完成
     */
    COMPLETED(2, "已完成"),

    /**
     * 已归档
     */
    ARCHIVED(3, "已归档");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static ChatTaskStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChatTaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
