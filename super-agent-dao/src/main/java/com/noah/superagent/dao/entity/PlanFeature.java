package com.noah.superagent.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 套餐特性描述对象
 * 用于 SubscriptionPlanEntity 中的 features 列表
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeature {
    /**
     * 功能描述文本
     */
    private String text;
    /**
     * 是否突出显示
     */
    private Boolean highlight;
    /**
     * 是否包含此功能 (默认true)
     */
    private Boolean included = true;
}
