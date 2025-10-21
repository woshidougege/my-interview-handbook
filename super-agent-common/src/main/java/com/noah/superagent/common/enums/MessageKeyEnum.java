package com.noah.superagent.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 国际化消息Key枚举
 * 统一管理所有国际化消息的key，避免硬编码字符串
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MessageKeyEnum {

    // ========== 订阅相关 ==========
    /**
     * 获取套餐列表成功
     */
    SUBSCRIPTION_PLANS_SUCCESS("subscription.plans.success"),
    
    /**
     * 获取当前订阅信息成功
     */
    SUBSCRIPTION_CURRENT_SUCCESS("subscription.current.success"),
    
    // ========== 套餐名称 ==========
    /**
     * 免费版套餐名称
     */
    PLAN_NAME_FREE("plan.name.FREE"),
    
    /**
     * 基础版套餐名称
     */
    PLAN_NAME_BASIC("plan.name.BASIC"),
    
    /**
     * 高级版套餐名称
     */
    PLAN_NAME_PREMIUM("plan.name.PREMIUM"),
    
    /**
     * 购买积分套餐名称
     */
    PLAN_NAME_CREDIT_PACK("plan.name.CREDIT_PACK"),
    
    /**
     * 开发者神仙版套餐名称
     */
    PLAN_NAME_DEVELOPER_GOD("plan.name.DEVELOPER_GOD"),
    
    ;

    /**
     * 消息key（对应properties文件中的key）
     */
    private final String key;

    /**
     * 获取消息key
     */
    @Override
    public String toString() {
        return this.key;
    }
}

