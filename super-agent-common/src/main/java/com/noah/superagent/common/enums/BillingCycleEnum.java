package com.noah.superagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计费周期枚举
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum BillingCycleEnum {

    /**
     * 按月计费
     */
    MONTHLY("monthly", "按月"),

    /**
     * 按年计费
     */
    YEARLY("yearly", "按年");

    /**
     * 计费周期代码
     */
    private final String code;

    /**
     * 计费周期描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 代码
     * @return 枚举值
     */
    public static BillingCycleEnum fromCode(String code) {
        if (code == null) {
            return MONTHLY; // 默认按月
        }
        
        for (BillingCycleEnum cycle : values()) {
            if (cycle.getCode().equals(code)) {
                return cycle;
            }
        }
        return MONTHLY; // 默认按月
    }
}
