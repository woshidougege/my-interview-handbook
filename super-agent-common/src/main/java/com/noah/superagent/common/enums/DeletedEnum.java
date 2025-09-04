package com.noah.superagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 逻辑删除枚举
 * 定义数据的删除状态
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DeletedEnum {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 对应的枚举，未找到则返回null
     */
    public static DeletedEnum valueOf(Integer value) {
        if (value == null) {
            return null;
        }
        for (DeletedEnum deleted : values()) {
            if (deleted.getValue().equals(value)) {
                return deleted;
            }
        }
        return null;
    }

    /**
     * 判断是否已删除
     *
     * @param value 状态值
     * @return true-已删除，false-未删除
     */
    public static boolean isDeleted(Integer value) {
        return DELETED.getValue().equals(value);
    }

    /**
     * 判断是否未删除
     *
     * @param value 状态值
     * @return true-未删除，false-已删除
     */
    public static boolean isNotDeleted(Integer value) {
        return NOT_DELETED.getValue().equals(value);
    }
}
