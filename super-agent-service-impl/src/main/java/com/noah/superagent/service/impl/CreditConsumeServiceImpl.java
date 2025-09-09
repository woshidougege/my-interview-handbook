package com.noah.superagent.service.impl;

import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.common.enums.ResponseCodeEnum;
import com.noah.superagent.common.exception.BusinessException;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import com.noah.superagent.dao.entity.UserCreditBalanceEntity;
import com.noah.superagent.dao.mapper.CreditTransactionMapper;
import com.noah.superagent.dao.mapper.UserCreditAccountMapper;
import com.noah.superagent.dao.mapper.UserCreditBalanceMapper;
import com.noah.superagent.service.CreditConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分消费服务实现
 * 
 * 处理积分扣费逻辑，按照有效期优先级顺序扣费
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditConsumeServiceImpl implements CreditConsumeService {

    private final UserCreditAccountMapper userCreditAccountMapper;
    private final UserCreditBalanceMapper userCreditBalanceMapper;
    private final CreditTransactionMapper creditTransactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse consumeCredits(Long userId, BigDecimal amount, String description, Long relatedOrderId) {
        log.info("开始扣费用户积分 - userId: {}, amount: {}, description: {}", userId, amount, description);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "扣费金额必须大于0");
        }
        
        // 1. 查询用户积分汇总账户
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND, "用户积分账户不存在");
        }
        
        // 2. 检查总积分是否足够
        if (creditAccount.getTotalBalance().compareTo(amount) < 0) {
            throw new BusinessException(ResponseCodeEnum.INSUFFICIENT_CREDITS, 
                    String.format("积分余额不足，当前余额: %s，需要: %s", creditAccount.getTotalBalance(), amount));
        }
        
        // 3. 获取用户各类型积分余额并按优先级排序
        List<UserCreditBalanceEntity> balanceList = userCreditBalanceMapper.selectByUserId(userId);
        Map<CreditTypeEnum, UserCreditBalanceEntity> balanceMap = balanceList.stream()
                .collect(Collectors.toMap(
                        UserCreditBalanceEntity::getCreditType,
                        balance -> balance
                ));
        
        // 4. 按优先级计算扣费方案
        List<CreditDeduction> deductions = calculateDeductions(balanceMap, amount);
        
        // 5. 更新积分汇总账户
        BigDecimal oldTotalBalance = creditAccount.getTotalBalance();
        BigDecimal newTotalBalance = oldTotalBalance.subtract(amount);
        BigDecimal newTotalSpent = creditAccount.getTotalSpent().add(amount);
        
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(creditAccount.getId());
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setTotalSpent(newTotalSpent);
        updateAccount.setVersion(creditAccount.getVersion());
        updateAccount.setUpdateBy(userId);
        
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(userId, updateAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分账户失败（可能并发冲突） - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分账户失败");
        }
        
        // 6. 分别更新各类型积分余额并记录交易
        for (CreditDeduction deduction : deductions) {
            UserCreditBalanceEntity balance = balanceMap.get(deduction.getType());
            if (balance == null) {
                continue; // 理论上不会发生
            }
            
            // 更新积分余额
            BigDecimal newBalance = balance.getBalance().subtract(deduction.getAmount());
            BigDecimal newBalanceTotalSpent = balance.getTotalSpent().add(deduction.getAmount());
            
            balance.setBalance(newBalance);
            balance.setTotalSpent(newBalanceTotalSpent);
            balance.setLastSpendTime(LocalDateTime.now());
            balance.setUpdateBy(userId);
            
            int balanceUpdateResult = userCreditBalanceMapper.updateBalanceByUserIdAndCreditType(
                    userId, deduction.getType(), balance);
            if (balanceUpdateResult <= 0) {
                log.error("更新用户积分余额失败 - userId: {}, creditType: {}", userId, deduction.getType());
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分余额失败");
            }
            
            // 记录积分交易记录
            CreditTransactionEntity transaction = new CreditTransactionEntity();
            transaction.setUserId(userId);
            transaction.setTransactionType(CreditTransactionTypeEnum.EXPENSE_TOKEN_USAGE);
            transaction.setCreditType(deduction.getType());
            transaction.setAmount(deduction.getAmount().negate()); // 负数表示支出
            transaction.setBalanceBefore(oldTotalBalance);
            transaction.setBalanceAfter(newTotalBalance);
            transaction.setDescription(description + " - " + deduction.getType().getDesc());
            transaction.setRelatedOrderId(relatedOrderId);
            transaction.setCreateBy(userId);
            
            int transactionResult = creditTransactionMapper.insertTransaction(transaction);
            if (transactionResult <= 0) {
                log.error("插入积分交易记录失败 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录交易失败");
            }
        }
        
        log.info("用户积分扣费成功 - userId: {}, 扣费金额: {}, 新余额: {}", userId, amount, newTotalBalance);
        
        // 7. 返回最新的积分信息
        UserCreditResponse response = new UserCreditResponse();
        response.setUserId(userId);
        response.setTotalBalance(newTotalBalance);
        response.setTotalEarned(creditAccount.getTotalEarned());
        response.setTotalSpent(newTotalSpent);
        
        // 设置各类型积分余额
        response.setDailyBalance(getBalanceSafely(balanceMap, CreditTypeEnum.DAILY));
        response.setActivityBalance(getBalanceSafely(balanceMap, CreditTypeEnum.ACTIVITY));
        response.setFreeBalance(getBalanceSafely(balanceMap, CreditTypeEnum.NEW_USER));
        response.setPermanentBalance(getBalanceSafely(balanceMap, CreditTypeEnum.PERMANENT));
        
        return response;
    }

    @Override
    public boolean hasEnoughCredits(Long userId, BigDecimal amount) {
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            return false;
        }
        return creditAccount.getTotalBalance().compareTo(amount) >= 0;
    }

    @Override
    public String previewCreditConsumption(Long userId, BigDecimal amount) {
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            return "用户积分账户不存在";
        }
        
        if (creditAccount.getTotalBalance().compareTo(amount) < 0) {
            return "积分余额不足，当前余额: " + creditAccount.getTotalBalance() + "，需要: " + amount;
        }
        
        // 获取用户各类型积分余额
        List<UserCreditBalanceEntity> balanceList = userCreditBalanceMapper.selectByUserId(userId);
        Map<CreditTypeEnum, UserCreditBalanceEntity> balanceMap = balanceList.stream()
                .collect(Collectors.toMap(
                        UserCreditBalanceEntity::getCreditType,
                        balance -> balance
                ));
        
        List<CreditDeduction> deductions = calculateDeductions(balanceMap, amount);
        StringBuilder preview = new StringBuilder();
        preview.append("积分扣费计划：\n");
        
        for (CreditDeduction deduction : deductions) {
            preview.append("- ")
                   .append(deduction.getType().getFullDesc())
                   .append(": ")
                   .append(deduction.getAmount())
                   .append(" 积分\n");
        }
        
        return preview.toString();
    }

    /**
     * 计算积分扣费方案
     * 按照积分类型的消费优先级扣费：数字越小优先级越高
     */
    private List<CreditDeduction> calculateDeductions(Map<CreditTypeEnum, UserCreditBalanceEntity> balanceMap, BigDecimal totalAmount) {
        List<CreditDeduction> deductions = new ArrayList<>();
        BigDecimal remainingAmount = totalAmount;
        
        // 按消费优先级排序（数字越小优先级越高）
        List<CreditTypeEnum> sortedTypes = balanceMap.keySet().stream()
                .filter(type -> balanceMap.get(type).getBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(CreditTypeEnum::getConsumePriority))
                .collect(Collectors.toList());
        
        for (CreditTypeEnum creditType : sortedTypes) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            UserCreditBalanceEntity balance = balanceMap.get(creditType);
            BigDecimal availableBalance = balance.getBalance();
            
            if (availableBalance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deductAmount = remainingAmount.min(availableBalance);
                deductions.add(new CreditDeduction(creditType, deductAmount));
                remainingAmount = remainingAmount.subtract(deductAmount);
                
                log.debug("积分扣费计划 - 类型: {}, 扣费金额: {}, 剩余需扣: {}", 
                         creditType.getDesc(), deductAmount, remainingAmount);
            }
        }
        
        // 验证是否能完全扣费
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.error("积分余额不足以完成扣费 - 剩余需扣: {}", remainingAmount);
            throw new BusinessException(ResponseCodeEnum.INSUFFICIENT_CREDITS, "积分余额不足");
        }
        
        return deductions;
    }

    /**
     * 安全获取余额，不存在时返回0
     */
    private BigDecimal getBalanceSafely(Map<CreditTypeEnum, UserCreditBalanceEntity> balanceMap, CreditTypeEnum creditType) {
        UserCreditBalanceEntity balance = balanceMap.get(creditType);
        return balance != null ? balance.getBalance() : BigDecimal.ZERO;
    }

    /**
     * 积分扣费项
     */
    private static class CreditDeduction {
        private final CreditTypeEnum type;
        private final BigDecimal amount;

        public CreditDeduction(CreditTypeEnum type, BigDecimal amount) {
            this.type = type;
            this.amount = amount;
        }

        public CreditTypeEnum getType() {
            return type;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
