package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.CreditTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户积分余额明细表
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user_credit_balance")
public class UserCreditBalanceEntity extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 积分类型代码
     */
    private CreditTypeEnum creditType;

    /**
     * 余额
     */
    private BigDecimal balance;

    /**
     * 累计获得
     */
    private BigDecimal totalEarned;

    /**
     * 累计消费
     */
    private BigDecimal totalSpent;

    /**
     * 最后获得时间
     */
    private LocalDateTime lastEarnTime;

    /**
     * 最后消费时间
     */
    private LocalDateTime lastSpendTime;

    /**
     * 版本号（乐观锁）
     */
    @Column(version = true)
    private Integer version;

    /**
     * 检查余额是否足够
     */
    public boolean hasEnoughBalance(BigDecimal amount) {
        return balance != null && balance.compareTo(amount) >= 0;
    }

    /**
     * 增加余额
     */
    public void addBalance(BigDecimal amount) {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        balance = balance.add(amount);
        
        if (totalEarned == null) {
            totalEarned = BigDecimal.ZERO;
        }
        totalEarned = totalEarned.add(amount);
        
        lastEarnTime = LocalDateTime.now();
    }

    /**
     * 扣减余额
     */
    public void subtractBalance(BigDecimal amount) {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        balance = balance.subtract(amount);
        
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }
        totalSpent = totalSpent.add(amount);
        
        lastSpendTime = LocalDateTime.now();
    }
}
