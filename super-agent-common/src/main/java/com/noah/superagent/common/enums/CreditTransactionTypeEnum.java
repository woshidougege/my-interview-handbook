package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分交易类型枚举
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CreditTransactionTypeEnum implements BaseEnum<Integer> {

    /**
     * 收入 - 免费套餐每日赠送
     */
    INCOME_FREE_PLAN_DAILY(1, "免费套餐每日赠送"),

    /**
     * 收入 - PRO套餐赠送
     */
    INCOME_PRO_PLAN(2, "PRO套餐赠送"),

    /**
     * 收入 - PRO+套餐赠送
     */
    INCOME_PRO_PLUS_PLAN(3, "PRO+套餐赠送"),

    /**
     * 支出 - Token消费
     */
    EXPENSE_TOKEN_USAGE(4, "Token消费"),

    /**
     * 支出 - 积分过期清零
     */
    EXPENSE_EXPIRED_CLEAR(5, "过期清零");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static CreditTransactionTypeEnum getByCode(Integer code) {
        return BaseEnum.getByCode(CreditTransactionTypeEnum.class, code);
    }

    /**
     * 是否为收入类型
     */
    public boolean isIncome() {
        return this == INCOME_FREE_PLAN_DAILY || this == INCOME_PRO_PLAN || this == INCOME_PRO_PLUS_PLAN;
    }

    /**
     * 是否为支出类型
     */
    public boolean isExpense() {
        return this == EXPENSE_TOKEN_USAGE || this == EXPENSE_EXPIRED_CLEAR;
    }
}
