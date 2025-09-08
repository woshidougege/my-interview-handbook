package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收藏状态枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum FavoriteEnum {

    /**
     * 未收藏
     */
    NOT_FAVORITE(0, "未收藏"),

    /**
     * 已收藏
     */
    FAVORITE(1, "已收藏");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static FavoriteEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FavoriteEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
