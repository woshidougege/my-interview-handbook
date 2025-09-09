package com.noah.superagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.common.enums.ResponseCodeEnum;
import com.noah.superagent.common.exception.BusinessException;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import com.noah.superagent.dao.mapper.CreditTransactionMapper;
import com.noah.superagent.dao.mapper.UserCreditAccountMapper;
import com.noah.superagent.service.CreditConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 积分消费服务实现
 * 
 * 处理积分扣费逻辑，按照有效期优先级顺序扣费
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditConsumeServiceImpl implements CreditConsumeService {

    private final UserCreditAccountMapper userCreditAccountMapper;
    private final CreditTransactionMapper creditTransactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse consumeCredits(Long userId, BigDecimal amount, String description, Long relatedOrderId) {
        log.info("开始扣费用户积分 - userId: {}, amount: {}, description: {}", userId, amount, description);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "扣费金额必须大于0");
        }
        
        // 查询用户积分账户
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND, "用户积分账户不存在");
        }
        
        // 检查总积分是否足够
        if (!hasEnoughCredits(userId, amount)) {
            throw new BusinessException(ResponseCodeEnum.INSUFFICIENT_CREDITS, "积分余额不足");
        }
        
        // 按优先级扣费
        List<CreditDeduction> deductions = calculateDeductions(creditAccount, amount);
        BigDecimal totalDeducted = BigDecimal.ZERO;
        
        // 准备更新数据
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(creditAccount.getId());
        updateAccount.setVersion(creditAccount.getVersion());
        updateAccount.setUpdateBy(userId);
        
        BigDecimal newTotalBalance = creditAccount.getTotalBalance().subtract(amount);
        BigDecimal newTotalSpent = creditAccount.getTotalSpent().add(amount);
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setTotalSpent(newTotalSpent);
        
        // 分别更新各类积分余额
        for (CreditDeduction deduction : deductions) {
            switch (deduction.getType()) {
                case DAILY:
                    BigDecimal newDailyBalance = creditAccount.getDailyBalance().subtract(deduction.getAmount());
                    updateAccount.setDailyBalance(newDailyBalance);
                    break;
                case ACTIVITY:
                    BigDecimal newActivityBalance = creditAccount.getActivityBalance().subtract(deduction.getAmount());
                    updateAccount.setActivityBalance(newActivityBalance);
                    break;
                case FREE:
                    BigDecimal newFreeBalance = creditAccount.getFreeBalance().subtract(deduction.getAmount());
                    updateAccount.setFreeBalance(newFreeBalance);
                    break;
                case PERMANENT:
                    BigDecimal newPermanentBalance = creditAccount.getPermanentBalance().subtract(deduction.getAmount());
                    updateAccount.setPermanentBalance(newPermanentBalance);
                    break;
            }
            totalDeducted = totalDeducted.add(deduction.getAmount());
        }
        
        // 验证扣费总额
        if (totalDeducted.compareTo(amount) != 0) {
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR, "积分扣费计算错误");
        }
        
        // 更新积分账户（使用乐观锁）
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(userId, updateAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分账户失败（可能并发冲突） - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分账户失败");
        }
        
        // 记录积分交易记录
        for (CreditDeduction deduction : deductions) {
            CreditTransactionEntity transaction = new CreditTransactionEntity();
            transaction.setUserId(userId);
            transaction.setTransactionType(CreditTransactionTypeEnum.EXPENSE_TOKEN_USAGE);
            transaction.setAmount(deduction.getAmount().negate()); // 负数表示支出
            transaction.setBalanceBefore(creditAccount.getTotalBalance());
            transaction.setBalanceAfter(newTotalBalance);
            transaction.setDescription(description + " - " + deduction.getType().getDesc());
            transaction.setRelatedOrderId(relatedOrderId);
            transaction.setCreateBy(userId);
            
            int transactionResult = creditTransactionMapper.insert(transaction);
            if (transactionResult <= 0) {
                log.error("插入积分交易记录失败 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录交易失败");
            }
        }
        
        // 构造返回结果
        UserCreditResponse response = BeanUtil.copyProperties(updateAccount, UserCreditResponse.class);
        response.setUserId(userId);
        
        log.info("用户积分扣费成功 - userId: {}, 扣费金额: {}, 新余额: {}", userId, amount, newTotalBalance);
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
        
        if (!hasEnoughCredits(userId, amount)) {
            return "积分余额不足，当前余额: " + creditAccount.getTotalBalance() + "，需要: " + amount;
        }
        
        List<CreditDeduction> deductions = calculateDeductions(creditAccount, amount);
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
     */
    private List<CreditDeduction> calculateDeductions(UserCreditAccountEntity account, BigDecimal totalAmount) {
        List<CreditDeduction> deductions = new ArrayList<>();
        BigDecimal remainingAmount = totalAmount;
        
        // 按优先级扣费：当日积分 → 活动积分 → 免费积分 → 永久积分
        
        // 1. 扣当日积分（1天有效）
        BigDecimal dailyBalance = account.getDailyBalance() != null ? account.getDailyBalance() : BigDecimal.ZERO;
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && dailyBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deductAmount = remainingAmount.min(dailyBalance);
            deductions.add(new CreditDeduction(CreditTypeEnum.DAILY, deductAmount));
            remainingAmount = remainingAmount.subtract(deductAmount);
        }
        
        // 2. 扣活动积分（90天有效）
        BigDecimal activityBalance = account.getActivityBalance() != null ? account.getActivityBalance() : BigDecimal.ZERO;
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && activityBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deductAmount = remainingAmount.min(activityBalance);
            deductions.add(new CreditDeduction(CreditTypeEnum.ACTIVITY, deductAmount));
            remainingAmount = remainingAmount.subtract(deductAmount);
        }
        
        // 3. 扣免费积分（90天有效）
        BigDecimal freeBalance = account.getFreeBalance() != null ? account.getFreeBalance() : BigDecimal.ZERO;
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && freeBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deductAmount = remainingAmount.min(freeBalance);
            deductions.add(new CreditDeduction(CreditTypeEnum.FREE, deductAmount));
            remainingAmount = remainingAmount.subtract(deductAmount);
        }
        
        // 4. 扣永久积分（无期限）
        BigDecimal permanentBalance = account.getPermanentBalance() != null ? account.getPermanentBalance() : BigDecimal.ZERO;
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && permanentBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deductAmount = remainingAmount.min(permanentBalance);
            deductions.add(new CreditDeduction(CreditTypeEnum.PERMANENT, deductAmount));
            remainingAmount = remainingAmount.subtract(deductAmount);
        }
        
        return deductions;
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
