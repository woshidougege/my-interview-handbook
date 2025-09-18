package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日登录记录表
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user_daily_login")
public class UserDailyLoginEntity extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录日期
     */
    private LocalDate loginDate;

    /**
     * 当日登录次数
     */
    private Integer loginCount;

    /**
     * 当日首次登录时间
     */
    private LocalDateTime firstLoginTime;

    /**
     * 当日积分是否已发放
     */
    private Boolean dailyCreditsGranted;

    /**
     * 当日发放的积分数量
     */
    private BigDecimal dailyCreditsAmount;
}
