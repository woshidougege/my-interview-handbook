package com.noah.superagent.service.impl;

import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import com.noah.superagent.dao.mapper.CreditTransactionMapper;
import com.noah.superagent.dao.mapper.UserCreditAccountMapper;
import com.noah.superagent.service.CreditExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分过期清理服务实现
 * 
 * 处理不同类型积分的过期清理逻辑
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditExpiryServiceImpl implements CreditExpiryService {

    private final UserCreditAccountMapper userCreditAccountMapper;
    private final CreditTransactionMapper creditTransactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredDailyCredits() {
        log.info("开始清理过期的当日积分（1天前创建的）");
        
        // 计算1天前的时间点
        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(1);
        
        // 查询有当日积分余额的用户
        List<UserCreditAccountEntity> accounts = userCreditAccountMapper.selectUsersWithDailyBalance();
        
        int processedCount = 0;
        for (UserCreditAccountEntity account : accounts) {
            if (account.getDailyBalance() != null && account.getDailyBalance().compareTo(BigDecimal.ZERO) > 0) {
                // 查询该用户过期的当日积分交易记录
                BigDecimal expiredAmount = calculateExpiredDailyCredits(account.getUserId(), expireThreshold);
                
                if (expiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 扣减过期的当日积分
                    clearExpiredDailyCredits(account, expiredAmount);
                    processedCount++;
                }
            }
        }
        
        log.info("当日积分过期清理完成 - 处理用户数: {}", processedCount);
        return processedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredActivityCredits() {
        log.info("开始清理过期的活动积分（90天前创建的）");
        
        // 计算90天前的时间点
        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(90);
        
        // 查询有活动积分余额的用户
        List<UserCreditAccountEntity> accounts = userCreditAccountMapper.selectUsersWithActivityBalance();
        
        int processedCount = 0;
        for (UserCreditAccountEntity account : accounts) {
            if (account.getActivityBalance() != null && account.getActivityBalance().compareTo(BigDecimal.ZERO) > 0) {
                // 查询该用户过期的活动积分交易记录
                BigDecimal expiredAmount = calculateExpiredActivityCredits(account.getUserId(), expireThreshold);
                
                if (expiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 扣减过期的活动积分
                    clearExpiredActivityCredits(account, expiredAmount);
                    processedCount++;
                }
            }
        }
        
        log.info("活动积分过期清理完成 - 处理用户数: {}", processedCount);
        return processedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredFreeCredits() {
        log.info("开始清理过期的免费积分（90天前创建的）");
        
        // 计算90天前的时间点
        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(90);
        
        // 查询有免费积分余额的用户
        List<UserCreditAccountEntity> accounts = userCreditAccountMapper.selectUsersWithFreeBalance();
        
        int processedCount = 0;
        for (UserCreditAccountEntity account : accounts) {
            if (account.getFreeBalance() != null && account.getFreeBalance().compareTo(BigDecimal.ZERO) > 0) {
                // 查询该用户过期的免费积分交易记录
                BigDecimal expiredAmount = calculateExpiredFreeCredits(account.getUserId(), expireThreshold);
                
                if (expiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 扣减过期的免费积分
                    clearExpiredFreeCredits(account, expiredAmount);
                    processedCount++;
                }
            }
        }
        
        log.info("免费积分过期清理完成 - 处理用户数: {}", processedCount);
        return processedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cleanupExpiredCreditsForUser(Long userId) {
        log.info("开始清理用户过期积分 - userId: {}", userId);
        
        try {
            // 查询用户积分账户
            UserCreditAccountEntity account = userCreditAccountMapper.selectByUserId(userId);
            if (account == null) {
                log.warn("用户积分账户不存在 - userId: {}", userId);
                return false;
            }
            
            boolean hasExpiredCredits = false;
            
            // 清理过期当日积分
            LocalDateTime dailyExpireThreshold = LocalDateTime.now().minusDays(1);
            BigDecimal expiredDailyAmount = calculateExpiredDailyCredits(userId, dailyExpireThreshold);
            if (expiredDailyAmount.compareTo(BigDecimal.ZERO) > 0) {
                clearExpiredDailyCredits(account, expiredDailyAmount);
                hasExpiredCredits = true;
            }
            
            // 清理过期活动积分和免费积分
            LocalDateTime longTermExpireThreshold = LocalDateTime.now().minusDays(90);
            
            BigDecimal expiredActivityAmount = calculateExpiredActivityCredits(userId, longTermExpireThreshold);
            if (expiredActivityAmount.compareTo(BigDecimal.ZERO) > 0) {
                clearExpiredActivityCredits(account, expiredActivityAmount);
                hasExpiredCredits = true;
            }
            
            BigDecimal expiredFreeAmount = calculateExpiredFreeCredits(userId, longTermExpireThreshold);
            if (expiredFreeAmount.compareTo(BigDecimal.ZERO) > 0) {
                clearExpiredFreeCredits(account, expiredFreeAmount);
                hasExpiredCredits = true;
            }
            
            if (hasExpiredCredits) {
                log.info("用户过期积分清理完成 - userId: {}", userId);
            } else {
                log.info("用户无过期积分需要清理 - userId: {}", userId);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("清理用户过期积分失败 - userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 计算过期的当日积分数量
     */
    private BigDecimal calculateExpiredDailyCredits(Long userId, LocalDateTime expireThreshold) {
        // 查询过期的当日积分交易记录（INCOME_FREE_PLAN_DAILY类型，创建时间早于阈值的）
        List<CreditTransactionEntity> expiredTransactions = creditTransactionMapper.selectExpiredTransactions(
                userId, CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY, expireThreshold);
        
        BigDecimal totalExpired = BigDecimal.ZERO;
        for (CreditTransactionEntity transaction : expiredTransactions) {
            totalExpired = totalExpired.add(transaction.getAmount());
        }
        
        return totalExpired;
    }

    /**
     * 计算过期的活动积分数量
     */
    private BigDecimal calculateExpiredActivityCredits(Long userId, LocalDateTime expireThreshold) {
        // TODO: 需要根据实际的活动积分交易类型来查询
        // 这里暂时返回0，待后续实现分享奖励等活动积分类型后再完善
        return BigDecimal.ZERO;
    }

    /**
     * 计算过期的免费积分数量
     */
    private BigDecimal calculateExpiredFreeCredits(Long userId, LocalDateTime expireThreshold) {
        // 查询过期的免费积分交易记录（新用户赠送类型，创建时间早于阈值的）
        // TODO: 需要新增新用户赠送类型的枚举值
        return BigDecimal.ZERO;
    }

    /**
     * 清理过期的当日积分
     */
    private void clearExpiredDailyCredits(UserCreditAccountEntity account, BigDecimal expiredAmount) {
        log.info("清理用户过期当日积分 - userId: {}, expiredAmount: {}", account.getUserId(), expiredAmount);
        
        // 准备更新数据
        BigDecimal currentDailyBalance = account.getDailyBalance();
        BigDecimal newDailyBalance = currentDailyBalance.subtract(expiredAmount);
        if (newDailyBalance.compareTo(BigDecimal.ZERO) < 0) {
            newDailyBalance = BigDecimal.ZERO;
        }
        
        BigDecimal newTotalBalance = account.getTotalBalance().subtract(expiredAmount);
        
        // 更新积分账户
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(account.getId());
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setDailyBalance(newDailyBalance);
        updateAccount.setVersion(account.getVersion());
        updateAccount.setUpdateBy(1L); // 系统操作
        
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(account.getUserId(), updateAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分账户失败 - userId: {}", account.getUserId());
            throw new RuntimeException("更新积分账户失败");
        }
        
        // 记录过期清零交易
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(account.getUserId());
        transaction.setTransactionType(CreditTransactionTypeEnum.EXPENSE_EXPIRED_CLEAR);
        transaction.setAmount(expiredAmount.negate()); // 负数表示支出
        transaction.setBalanceBefore(account.getTotalBalance());
        transaction.setBalanceAfter(newTotalBalance);
        transaction.setDescription("当日积分过期清零");
        transaction.setCreateBy(1L); // 系统操作
        
        creditTransactionMapper.insert(transaction);
        
        log.info("当日积分过期清零完成 - userId: {}, 清零金额: {}", account.getUserId(), expiredAmount);
    }

    /**
     * 清理过期的活动积分
     */
    private void clearExpiredActivityCredits(UserCreditAccountEntity account, BigDecimal expiredAmount) {
        // TODO: 实现活动积分过期清理逻辑
        log.info("活动积分过期清理逻辑待实现 - userId: {}", account.getUserId());
    }

    /**
     * 清理过期的免费积分
     */
    private void clearExpiredFreeCredits(UserCreditAccountEntity account, BigDecimal expiredAmount) {
        // TODO: 实现免费积分过期清理逻辑
        log.info("免费积分过期清理逻辑待实现 - userId: {}", account.getUserId());
    }
}
