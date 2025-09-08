package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
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
public enum DeletedEnum implements BaseEnum<Integer> {

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
    @EnumValue
    @JsonValue
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码值
     * @return 对应的枚举，未找到则返回null
     */
    @JsonCreator
    public static DeletedEnum getByCode(Integer code) {
        return BaseEnum.getByCode(DeletedEnum.class, code);
    }

    /**
     * 判断是否已删除
     *
     * @param code 编码值
     * @return true-已删除，false-未删除
     */
    public static boolean isDeleted(Integer code) {
        return DELETED.getCode().equals(code);
    }

    /**
     * 判断是否未删除
     *
     * @param code 编码值
     * @return true-未删除，false-已删除
     */
    public static boolean isNotDeleted(Integer code) {
        return NOT_DELETED.getCode().equals(code);
    }
}
