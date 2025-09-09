package com.noah.superagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.CreditTransactionResponse;
import com.noah.superagent.common.exception.BusinessException;
import com.noah.superagent.common.enums.ResponseCodeEnum;
import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import com.noah.superagent.dao.mapper.UserCreditAccountMapper;
import com.noah.superagent.dao.mapper.CreditTransactionMapper;
import com.noah.superagent.dao.mapper.UserMapper;
import com.noah.superagent.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户积分服务实现
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreditServiceImpl implements UserCreditService {

    private final UserCreditAccountMapper userCreditAccountMapper;
    private final CreditTransactionMapper creditTransactionMapper;
    private final UserMapper userMapper;

    // 免费套餐每日登录赠送积分数量
    private static final BigDecimal FREE_PLAN_DAILY_CREDITS = new BigDecimal("300");
    
    // 新用户注册赠送积分数量
    private static final BigDecimal NEW_USER_CREDITS = new BigDecimal("1000");
    
    // 用于防止重复赠送的日期格式
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public UserCreditResponse getUserCredit(Long userId) {
        log.info("查询用户积分账户信息 - userId: {}", userId);
        
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.warn("用户积分账户不存在 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND, "用户积分账户不存在");
        }

        UserCreditResponse response = BeanUtil.copyProperties(creditAccount, UserCreditResponse.class);
        
        // 计算永久积分余额（总余额 - 免费积分 - 包月积分）
        BigDecimal permanentBalance = creditAccount.getTotalBalance()
                .subtract(creditAccount.getFreeBalance())
                .subtract(creditAccount.getSubscriptionBalance());
        response.setPermanentBalance(permanentBalance);
        
        log.info("查询用户积分账户成功 - userId: {}, totalBalance: {}", userId, creditAccount.getTotalBalance());
        return response;
    }

    @Override
    public PageResponse<CreditTransactionResponse> getCreditTransactions(Long userId, Integer pageNum, Integer pageSize) {
        log.info("分页查询用户积分交易记录 - userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        
        Page<CreditTransactionEntity> page = new Page<>(pageNum, pageSize);
        Page<CreditTransactionEntity> resultPage = creditTransactionMapper.selectPageByUserId(page, userId);
        
        List<CreditTransactionResponse> responseList = resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        PageResponse<CreditTransactionResponse> pageResponse = new PageResponse<>(
                responseList, 
                resultPage.getTotalRow(), 
                pageNum, 
                pageSize
        );
        
        log.info("查询用户积分交易记录成功 - userId: {}, total: {}", userId, resultPage.getTotalRow());
        return pageResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse initUserCredit(Long userId) {
        log.info("初始化用户积分账户 - userId: {}", userId);
        
        // 检查是否已存在积分账户
        UserCreditAccountEntity existingAccount = userCreditAccountMapper.selectByUserId(userId);
        if (existingAccount != null) {
            log.warn("用户积分账户已存在 - userId: {}", userId);
            return BeanUtil.copyProperties(existingAccount, UserCreditResponse.class);
        }
        
        // 创建新的积分账户
        UserCreditAccountEntity creditAccount = new UserCreditAccountEntity();
        creditAccount.setUserId(userId);
        creditAccount.setTotalBalance(BigDecimal.ZERO);
        creditAccount.setFreeBalance(BigDecimal.ZERO);
        creditAccount.setSubscriptionBalance(BigDecimal.ZERO);
        creditAccount.setTotalEarned(BigDecimal.ZERO);
        creditAccount.setTotalSpent(BigDecimal.ZERO);
        creditAccount.setVersion(0);
        creditAccount.setCreateBy(userId);
        
        int result = userCreditAccountMapper.insertInitAccount(creditAccount);
        if (result <= 0) {
            log.error("初始化用户积分账户失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "初始化用户积分账户失败");
        }
        
        UserCreditResponse response = BeanUtil.copyProperties(creditAccount, UserCreditResponse.class);
        response.setPermanentBalance(BigDecimal.ZERO);
        
        log.info("初始化用户积分账户成功 - userId: {}", userId);
        return response;
    }

    @Override
    public boolean hasUserCredit(Long userId) {
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        return creditAccount != null;
    }

    @Override
    public Long getAvailableCredits(Long userId) {
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            return 0L;
        }
        return creditAccount.getTotalBalance().longValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse initFreePlanForUser(Long userId) {
        log.info("为用户分配免费体验套餐并初始化积分账户 - userId: {}", userId);
        
        // 检查用户是否存在
        if (userMapper.selectOneById(userId) == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        
        // 检查是否已存在积分账户
        UserCreditAccountEntity existingAccount = userCreditAccountMapper.selectByUserId(userId);
        if (existingAccount != null) {
            log.warn("用户积分账户已存在 - userId: {}", userId);
            return BeanUtil.copyProperties(existingAccount, UserCreditResponse.class);
        }
        
        // 创建新的积分账户并赠送新用户积分
        UserCreditAccountEntity creditAccount = new UserCreditAccountEntity();
        creditAccount.setUserId(userId);
        creditAccount.setTotalBalance(NEW_USER_CREDITS);
        creditAccount.setFreeBalance(NEW_USER_CREDITS); // 新用户1000积分属于免费积分
        creditAccount.setSubscriptionBalance(BigDecimal.ZERO);
        creditAccount.setTotalEarned(NEW_USER_CREDITS);
        creditAccount.setTotalSpent(BigDecimal.ZERO);
        creditAccount.setVersion(0);
        creditAccount.setCreateBy(userId);
        
        int result = userCreditAccountMapper.insertInitAccount(creditAccount);
        if (result <= 0) {
            log.error("初始化用户积分账户失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "初始化用户积分账户失败");
        }
        
        // 记录新用户赠送积分的交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY); // TODO: 需要新增新用户赠送类型
        transaction.setAmount(NEW_USER_CREDITS);
        transaction.setBalanceBefore(BigDecimal.ZERO);
        transaction.setBalanceAfter(NEW_USER_CREDITS);
        transaction.setDescription("新用户注册赠送积分（90天有效）");
        transaction.setExpireTime(LocalDateTime.now().plusDays(90)); // 90天后过期
        transaction.setCreateBy(userId);
        
        int transactionResult = creditTransactionMapper.insertTransaction(transaction);
        if (transactionResult <= 0) {
            log.warn("记录新用户积分交易失败 - userId: {}", userId);
        }
        
        // TODO: 这里应该创建用户订阅记录，绑定到免费套餐
        // 暂时先不实现，等后续完善套餐管理功能
        
        UserCreditResponse response = BeanUtil.copyProperties(creditAccount, UserCreditResponse.class);
        response.setPermanentBalance(BigDecimal.ZERO);
        
        log.info("用户免费套餐初始化成功 - userId: {}", userId);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse giveFreePlanDailyBonus(Long userId) {
        log.info("为用户发放免费套餐每日积分 - userId: {}, 积分数量: {}", userId, FREE_PLAN_DAILY_CREDITS);
        
        // 查询用户积分账户
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.warn("用户积分账户不存在 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND, "用户积分账户不存在");
        }
        
        // 检查今日是否已经发放过积分（防重）
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        
        List<CreditTransactionEntity> todayTransactions = creditTransactionMapper.selectByUserIdAndType(
            userId, CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY);
        
        boolean alreadyGivenToday = todayTransactions.stream()
                .anyMatch(transaction -> transaction.getDescription().contains(today));
        
        if (alreadyGivenToday) {
            log.info("用户今日已经发放过免费套餐积分 - userId: {}", userId);
            return BeanUtil.copyProperties(creditAccount, UserCreditResponse.class);
        }
        
        // 准备更新数据
        BigDecimal oldBalance = creditAccount.getTotalBalance();
        BigDecimal oldFreeBalance = creditAccount.getFreeBalance();
        BigDecimal newTotalBalance = oldBalance.add(FREE_PLAN_DAILY_CREDITS);
        BigDecimal newFreeBalance = oldFreeBalance.add(FREE_PLAN_DAILY_CREDITS);
        BigDecimal newTotalEarned = creditAccount.getTotalEarned().add(FREE_PLAN_DAILY_CREDITS);
        
        // 更新积分账户（使用乐观锁）
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(creditAccount.getId());
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setFreeBalance(newFreeBalance);
        updateAccount.setTotalEarned(newTotalEarned);
        updateAccount.setVersion(creditAccount.getVersion());
        updateAccount.setUpdateBy(userId);
        
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(userId, updateAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分账户失败（可能并发冲突） - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分账户失败");
        }
        
        // 记录积分交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY);
        transaction.setAmount(FREE_PLAN_DAILY_CREDITS);
        transaction.setBalanceBefore(oldBalance);
        transaction.setBalanceAfter(newTotalBalance);
        transaction.setDescription("每日登录赠送积分（24小时有效）");
        transaction.setExpireTime(LocalDateTime.now().plusDays(1)); // 1天后过期
        transaction.setCreateBy(userId);
        
        int transactionResult = creditTransactionMapper.insertTransaction(transaction);
        if (transactionResult <= 0) {
            log.error("插入积分交易记录失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录交易失败");
        }
        
        // 构造返回结果
        UserCreditResponse response = BeanUtil.copyProperties(updateAccount, UserCreditResponse.class);
        response.setUserId(userId);
        BigDecimal permanentBalance = newTotalBalance.subtract(newFreeBalance)
                .subtract(creditAccount.getSubscriptionBalance());
        response.setPermanentBalance(permanentBalance);
        
        log.info("免费套餐每日积分发放成功 - userId: {}, 发放积分: {}, 新余额: {}", 
                userId, FREE_PLAN_DAILY_CREDITS, newTotalBalance);
        return response;
    }

    @Override
    public String processFreePlanDailyBonusForAllUsers() {
        log.info("开始处理所有用户的免费套餐每日积分发放");
        
        // TODO: 这里应该查询所有免费套餐的用户，目前先查询所有有积分账户的用户
        // 后续完善套餐管理后，应该根据用户订阅状态来筛选
        
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        
        try {
            // 这里需要分页处理，避免一次性加载过多数据
            // 暂时简化实现，后续优化
            List<UserCreditAccountEntity> allAccounts = userCreditAccountMapper.selectAll();
            
            for (UserCreditAccountEntity account : allAccounts) {
                try {
                    giveFreePlanDailyBonus(account.getUserId());
                    successCount++;
                } catch (BusinessException e) {
                    if (e.getMessage().contains("今日已经发放过")) {
                        skipCount++;
                    } else {
                        failCount++;
                        log.error("用户每日积分发放失败 - userId: {}, 错误: {}", 
                                account.getUserId(), e.getMessage());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("用户每日积分发放异常 - userId: {}, 错误: {}", 
                            account.getUserId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("批量处理每日积分发放异常", e);
            return String.format("批量处理失败: %s", e.getMessage());
        }
        
        String result = String.format("批量处理完成 - 成功: %d, 跳过: %d, 失败: %d", 
                successCount, skipCount, failCount);
        log.info("免费套餐每日积分批量发放完成 - {}", result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse grantPaidPlanCredits(Long userId, Long creditAmount, Long orderId, String planName) {
        log.info("为用户发放付费套餐永久积分 - userId: {}, creditAmount: {}, orderId: {}, planName: {}", 
                userId, creditAmount, orderId, planName);
        
        BigDecimal credits = new BigDecimal(creditAmount);
        
        // 查询用户积分账户
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.warn("用户积分账户不存在 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND, "用户积分账户不存在");
        }
        
        // 准备更新数据
        BigDecimal oldBalance = creditAccount.getTotalBalance();
        BigDecimal oldSubscriptionBalance = creditAccount.getSubscriptionBalance();
        BigDecimal newTotalBalance = oldBalance.add(credits);
        BigDecimal newSubscriptionBalance = oldSubscriptionBalance.add(credits);
        BigDecimal newTotalEarned = creditAccount.getTotalEarned().add(credits);
        
        // 更新积分账户（使用乐观锁）
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(creditAccount.getId());
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setSubscriptionBalance(newSubscriptionBalance); // 付费积分计入订阅余额
        updateAccount.setTotalEarned(newTotalEarned);
        updateAccount.setVersion(creditAccount.getVersion());
        updateAccount.setUpdateBy(userId);
        
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(userId, updateAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分账户失败（可能并发冲突） - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分账户失败");
        }
        
        // 记录积分交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_PRO_PLAN); // 使用PRO套餐类型代表付费积分
        transaction.setAmount(credits);
        transaction.setBalanceBefore(oldBalance);
        transaction.setBalanceAfter(newTotalBalance);
        transaction.setDescription("付费套餐积分 - " + planName + "（永久有效）");
        transaction.setRelatedOrderId(orderId);
        // 付费积分无过期时间，永久有效
        transaction.setCreateBy(userId);
        
        int transactionResult = creditTransactionMapper.insertTransaction(transaction);
        if (transactionResult <= 0) {
            log.error("插入积分交易记录失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录交易失败");
        }
        
        // 构造返回结果
        UserCreditResponse response = BeanUtil.copyProperties(updateAccount, UserCreditResponse.class);
        response.setUserId(userId);
        BigDecimal permanentBalance = newTotalBalance.subtract(creditAccount.getFreeBalance())
                .subtract(newSubscriptionBalance);
        response.setPermanentBalance(permanentBalance);
        
        log.info("付费套餐积分发放成功 - userId: {}, 发放积分: {}, 新余额: {}", 
                userId, credits, newTotalBalance);
        return response;
    }

    /**
     * 转换积分交易记录为响应对象
     */
    private CreditTransactionResponse convertToResponse(CreditTransactionEntity entity) {
        CreditTransactionResponse response = BeanUtil.copyProperties(entity, CreditTransactionResponse.class);
        
        // 添加交易类型描述
        if (entity.getTransactionType() != null) {
            response.setTransactionTypeDesc(entity.getTransactionType().getDesc());
        }
        
        return response;
    }
}
