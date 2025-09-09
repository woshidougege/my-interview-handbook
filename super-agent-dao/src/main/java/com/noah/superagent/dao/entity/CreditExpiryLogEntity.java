package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.CreditTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 积分过期清理日志表
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_credit_expiry_log")
public class CreditExpiryLogEntity extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 积分类型代码
     */
    private CreditTypeEnum creditType;

    /**
     * 过期积分数量
     */
    private BigDecimal expiredAmount;

    /**
     * 过期日期
     */
    private LocalDate expireDate;

    /**
     * 原始积分交易记录ID
     */
    private Long originalTransactionId;

    /**
     * 处理时间
     */
    private LocalDateTime processedTime;

    /**
     * 创建过期日志
     */
    public static CreditExpiryLogEntity create(Long userId, CreditTypeEnum creditType, 
                                              BigDecimal expiredAmount, Long originalTransactionId) {
        CreditExpiryLogEntity log = new CreditExpiryLogEntity();
        log.setUserId(userId);
        log.setCreditType(creditType);
        log.setExpiredAmount(expiredAmount);
        log.setExpireDate(LocalDate.now());
        log.setOriginalTransactionId(originalTransactionId);
        log.setProcessedTime(LocalDateTime.now());
        return log;
    }
}
