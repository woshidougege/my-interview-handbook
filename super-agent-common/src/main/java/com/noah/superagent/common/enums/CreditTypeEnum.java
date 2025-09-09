package com.noah.superagent.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分类型枚举
 * 
 * 用于区分不同有效期和来源的积分类型
 *
 * @author Noah
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CreditTypeEnum implements BaseEnum<String> {

    /**
     * 当日积分（每日登录300积分，1天有效）
     */
    DAILY("daily", "当日积分", 1, 1),

    /**
     * 活动积分（分享奖励500积分等，90天有效）
     */
    ACTIVITY("activity", "活动积分", 90, 2),

    /**
     * 免费积分（新用户1000积分，90天有效）
     */
    FREE("free", "免费积分", 90, 3),

    /**
     * 永久积分（付费积分，无期限）
     */
    PERMANENT("permanent", "永久积分", 0, 4);

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
    private final Integer validityDays; // 有效期天数，0表示永久
    private final Integer consumePriority; // 消费优先级，数字越小优先级越高

    @JsonCreator
    public static CreditTypeEnum getByCode(String code) {
        return BaseEnum.getByCode(CreditTypeEnum.class, code);
    }

    /**
     * 是否为永久积分
     */
    public boolean isPermanent() {
        return this == PERMANENT;
    }

    /**
     * 是否为临时积分（有有效期）
     */
    public boolean isTemporary() {
        return validityDays > 0;
    }

    /**
     * 获取描述信息（包含有效期）
     */
    public String getFullDesc() {
        if (isPermanent()) {
            return desc + "（永久有效）";
        } else {
            return desc + "（" + validityDays + "天有效）";
        }
    }
}
