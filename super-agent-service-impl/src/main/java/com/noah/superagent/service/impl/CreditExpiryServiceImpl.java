package com.noah.superagent.service.impl;

import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import com.noah.superagent.dao.entity.UserCreditBalanceEntity;
import com.noah.superagent.dao.entity.CreditExpiryLogEntity;
import com.noah.superagent.dao.mapper.CreditTransactionMapper;
import com.noah.superagent.dao.mapper.UserCreditAccountMapper;
import com.noah.superagent.dao.mapper.UserCreditBalanceMapper;
import com.noah.superagent.dao.mapper.CreditExpiryLogMapper;
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
 * <p>
 * 处理不同类型积分的过期清理逻辑
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditExpiryServiceImpl implements CreditExpiryService {

    private final UserCreditAccountMapper userCreditAccountMapper;
    private final UserCreditBalanceMapper userCreditBalanceMapper;
    private final CreditTransactionMapper creditTransactionMapper;
    private final CreditExpiryLogMapper creditExpiryLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredDailyCredits() {
        log.info("开始清理过期的每日积分");
        return cleanupExpiredCreditsByType(CreditTypeEnum.DAILY);
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredActivityCredits() {
        log.info("开始清理过期的活动积分");
        return cleanupExpiredCreditsByType(CreditTypeEnum.ACTIVITY);
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredFreeCredits() {
        log.info("开始清理过期的新用户积分");
        return cleanupExpiredCreditsByType(CreditTypeEnum.FREE);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cleanupExpiredCreditsForUser(Long userId) {
        log.info("开始清理用户的过期积分 - userId: {}", userId);
        
        int totalProcessed = 0;
        
        try {
            // 分别清理各类型过期积分
            totalProcessed += cleanupExpiredUserCreditsByType(userId, CreditTypeEnum.DAILY);
            totalProcessed += cleanupExpiredUserCreditsByType(userId, CreditTypeEnum.ACTIVITY);
            totalProcessed += cleanupExpiredUserCreditsByType(userId, CreditTypeEnum.FREE);
            
            log.info("用户过期积分清理完成 - userId: {}, 处理类型数: {}", userId, totalProcessed);
            return true;
        } catch (Exception e) {
            log.error("清理用户过期积分失败 - userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 按积分类型清理过期积分
     */
    private int cleanupExpiredCreditsByType(CreditTypeEnum creditType) {
        // 获取积分类型配置
        Integer validityDays = creditType.getValidityDays();
        if (validityDays == null || validityDays <= 0) {
            log.debug("积分类型 {} 为永久有效，跳过清理", creditType);
            return 0;
        }
        
        // 计算过期时间阈值
        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(validityDays);
        log.debug("清理积分类型 {} 的过期积分，过期阈值: {}", creditType, expireThreshold);
        
        // 查询有该类型积分余额的用户
        List<UserCreditBalanceEntity> balances = userCreditBalanceMapper.selectUsersByCreditType(creditType);
        
        int processedCount = 0;
        for (UserCreditBalanceEntity balance : balances) {
            try {
                if (cleanupExpiredUserCreditsByType(balance.getUserId(), creditType) > 0) {
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("清理用户积分失败 - userId: {}, creditType: {}", balance.getUserId(), creditType, e);
            }
        }
        
        log.info("积分类型 {} 过期清理完成 - 处理用户数: {}", creditType, processedCount);
        return processedCount;
    }

    /**
     * 清理单个用户的特定类型过期积分
     */
    private int cleanupExpiredUserCreditsByType(Long userId, CreditTypeEnum creditType) {
        // 获取积分类型配置
        Integer validityDays = creditType.getValidityDays();
        if (validityDays == null || validityDays <= 0) {
            return 0; // 永久有效，无需清理
        }
        
        // 计算过期时间阈值
        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(validityDays);
        
        // 查询用户该类型积分余额
        UserCreditBalanceEntity balance = userCreditBalanceMapper.selectByUserIdAndCreditType(userId, creditType);
        if (balance == null || balance.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return 0; // 无余额，无需清理
        }
        
        // 查询过期的积分交易记录
        List<CreditTransactionEntity> expiredTransactions = creditTransactionMapper.selectExpiredTransactions(
                userId, getTransactionTypeByCredit(creditType), expireThreshold);
        
        if (expiredTransactions.isEmpty()) {
            return 0; // 无过期交易，无需清理
        }
        
        // 计算需要清理的积分总额
        BigDecimal totalExpiredAmount = expiredTransactions.stream()
                .map(CreditTransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalExpiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0; // 无需清理
        }
        
        // 实际清理的金额不能超过当前余额
        BigDecimal actualExpiredAmount = totalExpiredAmount.min(balance.getBalance());
        
        if (actualExpiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0; // 无需清理
        }
        
        // 更新用户积分汇总账户
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.error("用户积分账户不存在 - userId: {}", userId);
            return 0;
        }
        
        BigDecimal newTotalBalance = creditAccount.getTotalBalance().subtract(actualExpiredAmount);
        
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(creditAccount.getId());
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setVersion(creditAccount.getVersion());
        updateAccount.setUpdateBy(-1L); // 系统清理
        
        int accountUpdateResult = userCreditAccountMapper.updateBalanceByUserId(userId, updateAccount);
        if (accountUpdateResult <= 0) {
            log.error("更新用户积分汇总账户失败 - userId: {}", userId);
            return 0;
        }
        
        // 更新用户积分余额明细
        BigDecimal newBalance = balance.getBalance().subtract(actualExpiredAmount);
        balance.setBalance(newBalance);
        balance.setUpdateBy(-1L); // 系统清理
        
        int balanceUpdateResult = userCreditBalanceMapper.updateBalanceByUserIdAndCreditType(
                userId, creditType, balance);
        if (balanceUpdateResult <= 0) {
            log.error("更新用户积分余额明细失败 - userId: {}, creditType: {}", userId, creditType);
            return 0;
        }
        
        // 记录过期清理交易记录
        CreditTransactionEntity expiredTransaction = new CreditTransactionEntity();
        expiredTransaction.setUserId(userId);
        expiredTransaction.setTransactionType(CreditTransactionTypeEnum.EXPENSE_EXPIRED_CLEAR);
        expiredTransaction.setCreditType(creditType);
        expiredTransaction.setAmount(actualExpiredAmount.negate()); // 负数表示支出
        expiredTransaction.setBalanceBefore(creditAccount.getTotalBalance());
        expiredTransaction.setBalanceAfter(newTotalBalance);
        expiredTransaction.setDescription(creditType.getDesc() + "过期清理");
        expiredTransaction.setCreateBy(-1L); // 系统清理
        
        int transactionResult = creditTransactionMapper.insertTransaction(expiredTransaction);
        if (transactionResult <= 0) {
            log.warn("记录过期积分交易失败 - userId: {}, creditType: {}", userId, creditType);
        }
        
        // 记录过期清理日志
        CreditExpiryLogEntity expiryLog = CreditExpiryLogEntity.create(
                userId, creditType, actualExpiredAmount, 
                expiredTransactions.get(0).getId()); // 使用第一个过期交易的ID作为关联
        expiryLog.setCreateBy(-1L); // 系统清理
        
        int logResult = creditExpiryLogMapper.insert(expiryLog);
        if (logResult <= 0) {
            log.warn("记录过期清理日志失败 - userId: {}, creditType: {}", userId, creditType);
        }
        
        log.debug("清理用户过期积分成功 - userId: {}, creditType: {}, 清理金额: {}, 新余额: {}", 
                 userId, creditType, actualExpiredAmount, newTotalBalance);
        
        return 1;
    }

    /**
     * 根据积分类型获取对应的交易类型
     */
    private CreditTransactionTypeEnum getTransactionTypeByCredit(CreditTypeEnum creditType) {
        switch (creditType) {
            case ACTIVITY:
                // TODO: 需要添加活动积分交易类型
                return CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY; // 临时使用
            case FREE:
                // TODO: 需要添加新用户积分交易类型  
                return CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY; // 临时使用
            case PAID:
                return CreditTransactionTypeEnum.INCOME_PRO_PLAN;
            case DAILY:
            default:
                return CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY;
        }
    }
}