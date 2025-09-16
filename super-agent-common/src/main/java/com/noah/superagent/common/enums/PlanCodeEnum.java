package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 套餐代码枚举
 * 
 * 用于标识不同的订阅套餐类型，采用英文代码便于前端处理和国际化
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PlanCodeEnum implements BaseEnum<String> {

    /**
     * 免费版（新用户赠送1000积分 + 每日300积分）
     */
    FREE("free", "免费版"),

    /**
     * 基础版（一次性1900永久积分）
     */
    BASIC("basic", "基础版"),

    /**
     * 高级版（一次性19000永久积分）
     */
    PREMIUM("premium", "高级版"),

    /**
     * 额外购买积分（10000永久积分，无订阅期限）
     */
    CREDIT_PACK("credit_pack", "额外购买积分");

    @EnumValue
    @JsonValue
    private final String code;
    private final String displayName;

    @JsonCreator
    public static PlanCodeEnum getByCode(String code) {
        return BaseEnum.getByCode(PlanCodeEnum.class, code);
    }

    @Override
    public String getDesc() {
        return displayName;
    }

    /**
     * 是否为免费套餐
     */
    public boolean isFree() {
        return this == FREE;
    }

    /**
     * 是否为付费套餐
     */
    public boolean isPaid() {
        return this != FREE;
    }

    /**
     * 是否为积分包（不是订阅型套餐）
     */
    public boolean isCreditPack() {
        return this == CREDIT_PACK;
    }

    /**
     * 获取描述信息
     */
    public String getDescription() {
        switch (this) {
            case FREE:
                return "适合轻度使用的个人用户";
            case BASIC:
                return "适合中度使用的专业用户";
            case PREMIUM:
                return "适合重度使用的企业用户";
            case CREDIT_PACK:
                return "直接购买永久积分，无订阅期限";
            default:
                return displayName;
        }
    }
}
