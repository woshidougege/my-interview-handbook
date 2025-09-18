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
    CREDIT_PACK("credit_pack", "额外购买积分"),

    /**
     * 开发者神仙版（仅限开发测试使用，拥有近乎无限的权限）
     */
    DEVELOPER_GOD("developer_god", "开发者神仙版");

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
     * 获取套餐等级（用于升级/降级判断）
     * FREE(0) < BASIC(1) < PREMIUM(2)
     * CREDIT_PACK(-1) 不参与等级比较
     */
    public int getLevel() {
        switch (this) {
            case FREE:
                return 0;
            case BASIC:
                return 1;
            case PREMIUM:
                return 2;
            case CREDIT_PACK:
                return -1; // 积分包不参与等级比较
            default:
                return -1;
        }
    }

    /**
     * 是否可以升级到指定套餐
     * @param targetPlan 目标套餐
     * @return true-可以升级，false-不可以升级
     */
    public boolean canUpgradeTo(PlanCodeEnum targetPlan) {
        // 积分包总是可以购买
        if (targetPlan != null && targetPlan.isCreditPack()) {
            return true;
        }
        
        // 免费版永远不能被订阅
        if (targetPlan != null && targetPlan.isFree()) {
            return false;
        }
        
        // 其他情况：只能升级，不能降级
        if (targetPlan != null && this.getLevel() >= 0 && targetPlan.getLevel() >= 0) {
            return targetPlan.getLevel() > this.getLevel();
        }
        
        return false;
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
