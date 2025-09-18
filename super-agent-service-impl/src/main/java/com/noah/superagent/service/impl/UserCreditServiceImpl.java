package com.noah.superagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.common.config.BillingProperties;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.CreditTransactionResponse;
import com.noah.superagent.common.exception.BusinessException;
import com.noah.superagent.common.enums.ResponseCodeEnum;
import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import com.noah.superagent.dao.entity.UserCreditBalanceEntity;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import com.noah.superagent.dao.mapper.UserCreditAccountMapper;
import com.noah.superagent.dao.mapper.UserCreditBalanceMapper;
import com.noah.superagent.dao.mapper.CreditTransactionMapper;
import com.noah.superagent.dao.mapper.UserDailyLoginMapper;
import com.noah.superagent.dao.entity.UserDailyLoginEntity;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.service.UserSubscriptionService;
import com.noah.superagent.service.SubscriptionPlanService;
import com.noah.superagent.model.UserSubscriptionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户积分服务实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreditServiceImpl implements UserCreditService {

    private final UserCreditAccountMapper userCreditAccountMapper;
    private final UserCreditBalanceMapper userCreditBalanceMapper;
    private final CreditTransactionMapper creditTransactionMapper;
    private final UserDailyLoginMapper userDailyLoginMapper;
    private final BillingProperties billingProperties;
    private final UserSubscriptionService userSubscriptionService;
    private final SubscriptionPlanService subscriptionPlanService;

    /**
     * 根据用户当前套餐获取每日积分数量
     * @param userId 用户ID
     * @return 每日积分数量
     */
    private BigDecimal getDailyCreditsForUser(Long userId) {
        try {
            // 获取用户当前订阅套餐
            UserSubscriptionDTO subscription = userSubscriptionService.getCurrentActiveSubscription(userId);
            if (subscription != null && subscription.getPlanId() != null) {
                var plan = subscriptionPlanService.getPlanById(subscription.getPlanId());
                if (plan != null && plan.getDailyRefreshCredits() != null) {
                    log.debug("用户套餐每日积分 - userId: {}, planName: {}, dailyCredits: {}", 
                        userId, plan.getPlanName(), plan.getDailyRefreshCredits());
                    return BigDecimal.valueOf(plan.getDailyRefreshCredits());
                }
            }
            
            // 默认免费版积分（如果无法获取套餐信息）
            log.warn("无法获取用户套餐信息，使用默认免费版积分 - userId: {}", userId);
            return BigDecimal.valueOf(300);
        } catch (Exception e) {
            log.error("获取用户套餐每日积分失败 - userId: {}, 错误: {}", userId, e.getMessage());
            // 异常情况下返回免费版积分
            return BigDecimal.valueOf(300);
        }
    }
    
    /**
     * 获取新用户积分数量（根据产品需求：新用户一次性发放1000积分）
     */
    private BigDecimal getNewUserCredits() {
        return BigDecimal.valueOf(1000);
    }

    @Override
    public boolean canConsumeCredits(Long userId, BigDecimal amount) {
        log.debug("检查用户是否可以消费积分 - userId: {}, amount: {}", userId, amount);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            return false;
        }
        
        BigDecimal currentBalance = creditAccount.getTotalBalance();
        BigDecimal availableBalance = getAvailableBalance(currentBalance);
        
        boolean canConsume = availableBalance.compareTo(amount) >= 0;
        log.debug("积分消费检查结果 - userId: {}, 当前余额: {}, 可用余额: {}, 需要: {}, 结果: {}", 
                userId, currentBalance, availableBalance, amount, canConsume);
        
        return canConsume;
    }

    /**
     * 获取可用余额（包含透支额度）
     */
    private BigDecimal getAvailableBalance(BigDecimal currentBalance) {
        if (!billingProperties.getOverdraft().getEnabled()) {
            return currentBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : currentBalance;
        }
        BigDecimal maxOverdraft = billingProperties.getOverdraft().getMaxAmount();
        return currentBalance.add(maxOverdraft);
    }

    @Override
    public UserCreditResponse getUserCredit(Long userId) {
        log.info("查询用户积分账户信息 - userId: {}", userId);
        
        // 使用 Relations 注解自动关联查询积分余额明细
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserIdWithRelations(userId);
        if (creditAccount == null) {
            log.warn("用户积分账户不存在 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
        }

        // 通过 Relations 注解自动获取的积分余额明细
        List<UserCreditBalanceEntity> balanceList = creditAccount.getBalances();
        if (balanceList == null || balanceList.isEmpty()) {
            log.warn("用户积分余额明细为空 - userId: {}", userId);
            balanceList = List.of(); // 空列表，避免 NPE
        }
        
        log.info("查询到积分余额记录数量: {} - userId: {}", balanceList.size(), userId);
        
        for (UserCreditBalanceEntity balance : balanceList) {
            log.info("积分余额详情 - userId: {}, type: {}, balance: {}", userId, balance.getCreditType(), balance.getBalance());
        }
        
        Map<CreditTypeEnum, BigDecimal> balanceMap = balanceList.stream()
                .collect(Collectors.toMap(
                        UserCreditBalanceEntity::getCreditType,
                        UserCreditBalanceEntity::getBalance
                ));

        UserCreditResponse response = BeanUtil.copyProperties(creditAccount, UserCreditResponse.class);
        
        // 设置各类型积分余额
        response.setDailyBalance(balanceMap.getOrDefault(CreditTypeEnum.DAILY, BigDecimal.ZERO));
        response.setActivityBalance(balanceMap.getOrDefault(CreditTypeEnum.ACTIVITY, BigDecimal.ZERO));
        response.setFreeBalance(balanceMap.getOrDefault(CreditTypeEnum.FREE, BigDecimal.ZERO)); // 新用户积分作为免费积分
        response.setPermanentBalance(balanceMap.getOrDefault(CreditTypeEnum.PAID, BigDecimal.ZERO));
        
        log.info("响应积分详情 - userId: {}, daily: {}, activity: {}, free: {}, permanent: {}", 
                userId, response.getDailyBalance(), response.getActivityBalance(), 
                response.getFreeBalance(), response.getPermanentBalance());
        
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
    public boolean hasUserCredit(Long userId) {
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        return creditAccount != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse initFreePlanForUser(Long userId) {
        log.info("为用户分配免费体验套餐并初始化积分账户 - userId: {}", userId);
        
        // 注意：现在使用SSO认证，能调用到这里说明用户已通过认证，无需额外检查用户存在性
        
        // 检查是否已存在积分账户
        UserCreditAccountEntity existingAccount = userCreditAccountMapper.selectByUserId(userId);
        if (existingAccount != null) {
            log.warn("用户积分账户已存在 - userId: {}", userId);
            return getUserCredit(userId);
        }
        
        // 1. 创建积分汇总账户
        UserCreditAccountEntity creditAccount = new UserCreditAccountEntity();
        creditAccount.setUserId(userId);
        creditAccount.setTotalBalance(getNewUserCredits());
        creditAccount.setTotalEarned(getNewUserCredits());
        creditAccount.setTotalSpent(BigDecimal.ZERO);
        creditAccount.setVersion(0);
        creditAccount.setCreateBy(userId);
        
        int result = userCreditAccountMapper.insertInitAccount(creditAccount);
        if (result <= 0) {
            log.error("初始化用户积分账户失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "初始化用户积分账户失败");
        }
        
        // 2. 创建新用户积分余额记录
        UserCreditBalanceEntity newUserBalance = new UserCreditBalanceEntity();
        newUserBalance.setUserId(userId);
        newUserBalance.setCreditType(CreditTypeEnum.FREE);
        newUserBalance.setBalance(getNewUserCredits());
        newUserBalance.setTotalEarned(getNewUserCredits());
        newUserBalance.setTotalSpent(BigDecimal.ZERO);
        newUserBalance.setLastEarnTime(LocalDateTime.now());
        newUserBalance.setVersion(0);
        newUserBalance.setCreateBy(userId);
        
        int balanceResult = userCreditBalanceMapper.insertOrUpdate(newUserBalance);
        if (balanceResult <= 0) {
            log.error("创建用户积分余额记录失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "创建积分余额记录失败");
        }
        log.info("创建新用户积分余额记录成功 - userId: {}, type: {}, balance: {}", userId, CreditTypeEnum.FREE, getNewUserCredits());
        
        // 3. 记录新用户赠送积分的交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY); // TODO: 需要新增新用户赠送类型
        transaction.setCreditType(CreditTypeEnum.FREE);
        transaction.setAmount(getNewUserCredits());
        transaction.setBalanceBefore(BigDecimal.ZERO);
        transaction.setBalanceAfter(getNewUserCredits());
        transaction.setDescription("新用户注册赠送积分（90天有效）");
        transaction.setExpireTime(LocalDateTime.now().plusDays(90)); // 90天后过期
        transaction.setCreateBy(userId);
        
        int transactionResult = creditTransactionMapper.insertTransaction(transaction);
        if (transactionResult <= 0) {
            log.warn("记录新用户积分交易失败 - userId: {}", userId);
        }
        
        // TODO: 这里应该创建用户订阅记录，绑定到免费套餐
        // 暂时先不实现，等后续完善套餐管理功能
        
        // 4. 构建返回结果
        UserCreditResponse response = BeanUtil.copyProperties(creditAccount, UserCreditResponse.class);
        response.setDailyBalance(BigDecimal.ZERO);
        response.setActivityBalance(BigDecimal.ZERO);
        response.setFreeBalance(getNewUserCredits()); // 新用户积分作为免费积分显示
        response.setPermanentBalance(BigDecimal.ZERO);
        
        log.info("用户免费套餐初始化成功 - userId: {}", userId);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleUserLogin(Long userId) {
        LocalDate today = LocalDate.now();
        UserDailyLoginEntity dailyLogin;

        // 步骤 1: 记录或更新用户的每日登录活动
        try {
            dailyLogin = userDailyLoginMapper.selectByUserIdAndDate(userId, today);
            if (dailyLogin == null) {
                log.info("记录用户当日首次登录 - userId: {}", userId);
                dailyLogin = new UserDailyLoginEntity();
                dailyLogin.setUserId(userId);
                dailyLogin.setLoginDate(today);
                dailyLogin.setLoginCount(1);
                dailyLogin.setFirstLoginTime(LocalDateTime.now());
                dailyLogin.setDailyCreditsGranted(false);
                dailyLogin.setDailyCreditsAmount(BigDecimal.ZERO);
                dailyLogin.setCreateBy(userId);
                userDailyLoginMapper.insert(dailyLogin);
            } else {
                log.debug("更新用户当日登录次数 - userId: {}", userId);
                dailyLogin.setLoginCount(dailyLogin.getLoginCount() + 1);
                dailyLogin.setUpdateBy(userId);
                userDailyLoginMapper.update(dailyLogin);
            }
        } catch (Exception e) {
            log.error("记录用户每日登录失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            // 如果登录记录失败，则不应继续发放积分，抛出异常以回滚事务
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录用户登录失败");
        }

        // 步骤 2: 检查当日积分是否已发放
        if (Boolean.TRUE.equals(dailyLogin.getDailyCreditsGranted())) {
            log.info("用户今日已发放过每日积分，仅记录登录活动 - userId: {}", userId);
            return;
        }
        
        // 步骤 3: 执行积分发放逻辑
        log.info("用户登录时检查并发放每日积分（根据套餐配置） - userId: {}", userId);

        // 3.1 确保用户积分账户存在，不存在则初始化
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.info("用户积分账户不存在，先初始化为免费套餐账户 - userId: {}", userId);
            initFreePlanForUser(userId);
            creditAccount = userCreditAccountMapper.selectByUserId(userId);
            if (creditAccount == null) {
                log.error("初始化积分账户后仍然无法查询到 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
            }
        }
        
        // 3.2 根据用户套餐获取应得的每日积分
        BigDecimal dailyCredits = getDailyCreditsForUser(userId);
        if (dailyCredits.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("用户 {} 的当前套餐没有每日积分，跳过发放。", userId);
            // 即使不发放积分，也要标记为“已处理”，避免重复检查
            dailyLogin.setDailyCreditsGranted(true);
            dailyLogin.setUpdateBy(userId);
            userDailyLoginMapper.update(dailyLogin);
            return;
        }

        // 3.3 更新总账户余额
        BigDecimal oldTotalBalance = creditAccount.getTotalBalance();
        BigDecimal newTotalBalance = oldTotalBalance.add(dailyCredits);
        BigDecimal newTotalEarned = creditAccount.getTotalEarned().add(dailyCredits);
        creditAccount.setTotalBalance(newTotalBalance);
        creditAccount.setTotalEarned(newTotalEarned);
        creditAccount.setUpdateBy(userId);
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(userId, creditAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分总账户失败（可能并发冲突） - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分总账户失败");
        }

        // 3.4 更新或创建每日积分余额
        updateDailyCreditBalance(userId, dailyCredits);
        
        // 3.5 创建交易流水
        createDailyCreditTransaction(userId, dailyCredits, oldTotalBalance, newTotalBalance);

        // 步骤 4: 更新登录记录，标记积分为已发放
        dailyLogin.setDailyCreditsGranted(true);
        dailyLogin.setDailyCreditsAmount(dailyCredits);
        dailyLogin.setUpdateBy(userId);
        int finalUpdate = userDailyLoginMapper.update(dailyLogin);
        if (finalUpdate <= 0) {
            log.warn("更新每日登录记录为“已发放”状态失败 - userId: {}", userId);
        }

        log.info("每日积分发放成功 - userId: {}, 发放积分: {}, 新余额: {}", 
                userId, dailyCredits, newTotalBalance);
    }
    
    /**
     * 更新用户的每日积分余额（内部方法）
     */
    private void updateDailyCreditBalance(Long userId, BigDecimal dailyCredits) {
        UserCreditBalanceEntity dailyBalance = userCreditBalanceMapper.selectByUserIdAndCreditType(userId, CreditTypeEnum.DAILY);
        
        if (dailyBalance == null) {
            dailyBalance = new UserCreditBalanceEntity();
            dailyBalance.setUserId(userId);
            dailyBalance.setCreditType(CreditTypeEnum.DAILY);
            dailyBalance.setBalance(dailyCredits);
            dailyBalance.setTotalEarned(dailyCredits);
            dailyBalance.setTotalSpent(BigDecimal.ZERO);
            dailyBalance.setVersion(0);
            dailyBalance.setCreateBy(userId);
        } else {
            dailyBalance.setBalance(dailyCredits); // 每日积分是覆盖模式
            dailyBalance.setTotalEarned(dailyBalance.getTotalEarned().add(dailyCredits));
        }
        dailyBalance.setLastEarnTime(LocalDateTime.now());
        
        int result = userCreditBalanceMapper.insertOrUpdate(dailyBalance);
        if (result <= 0) {
            log.error("更新或创建用户每日积分余额失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新每日积分余额失败");
        }
    }
    
    /**
     * 创建每日积分的交易流水（内部方法）
     */
    private void createDailyCreditTransaction(Long userId, BigDecimal amount, BigDecimal before, BigDecimal after) {
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY);
        transaction.setCreditType(CreditTypeEnum.DAILY);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(before);
        transaction.setBalanceAfter(after);
        transaction.setDescription("每日登录赠送积分（根据套餐配置，24小时有效） - " + LocalDate.now());
        transaction.setExpireTime(LocalDateTime.now().plusDays(1));
        transaction.setCreateBy(userId);
        
        int result = creditTransactionMapper.insertTransaction(transaction);
        if (result <= 0) {
            log.error("插入每日积分交易记录失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录交易失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse grantPaidPlanCredits(Long userId, Long creditAmount, Long orderId, String planName) {
        log.info("为用户发放付费套餐永久积分 - userId: {}, creditAmount: {}, orderId: {}, planName: {}", 
                userId, creditAmount, orderId, planName);
        
        BigDecimal credits = new BigDecimal(creditAmount);
        
        // 1. 查询用户积分账户
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.warn("用户积分账户不存在 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
        }
        
        // 2. 更新积分汇总账户
        BigDecimal oldTotalBalance = creditAccount.getTotalBalance();
        BigDecimal newTotalBalance = oldTotalBalance.add(credits);
        BigDecimal newTotalEarned = creditAccount.getTotalEarned().add(credits);
        
        UserCreditAccountEntity updateAccount = new UserCreditAccountEntity();
        updateAccount.setId(creditAccount.getId());
        updateAccount.setTotalBalance(newTotalBalance);
        updateAccount.setTotalEarned(newTotalEarned);
        updateAccount.setVersion(creditAccount.getVersion());
        updateAccount.setUpdateBy(userId);
        
        int updateResult = userCreditAccountMapper.updateBalanceByUserId(userId, updateAccount);
        if (updateResult <= 0) {
            log.error("更新用户积分账户失败（可能并发冲突） - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新积分账户失败");
        }
        
        // 3. 创建或更新永久积分余额记录
        UserCreditBalanceEntity permanentBalance = userCreditBalanceMapper.selectByUserIdAndCreditType(userId, CreditTypeEnum.PAID);
        
        if (permanentBalance == null) {
            // 创建新的永久积分记录
            permanentBalance = new UserCreditBalanceEntity();
            permanentBalance.setUserId(userId);
            permanentBalance.setCreditType(CreditTypeEnum.PAID);
            permanentBalance.setBalance(credits);
            permanentBalance.setTotalEarned(credits);
            permanentBalance.setTotalSpent(BigDecimal.ZERO);
            permanentBalance.setLastEarnTime(LocalDateTime.now());
            permanentBalance.setVersion(0);
            permanentBalance.setCreateBy(userId);
            
            userCreditBalanceMapper.insertOrUpdate(permanentBalance);
        } else {
            // 更新现有的永久积分记录
            permanentBalance.setBalance(permanentBalance.getBalance().add(credits));
            permanentBalance.setTotalEarned(permanentBalance.getTotalEarned().add(credits));
            permanentBalance.setLastEarnTime(LocalDateTime.now());
            permanentBalance.setVersion(permanentBalance.getVersion());
            permanentBalance.setUpdateBy(userId);
            
            int balanceUpdateResult = userCreditBalanceMapper.updateBalanceByUserIdAndCreditType(
                userId, CreditTypeEnum.PAID, permanentBalance);
            if (balanceUpdateResult <= 0) {
                log.error("更新用户永久积分余额失败 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新永久积分余额失败");
            }
        }
        
        // 4. 记录积分交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_PRO_PLAN); // 使用PRO套餐类型代表付费积分
        transaction.setCreditType(CreditTypeEnum.PAID);
        transaction.setAmount(credits);
        transaction.setBalanceBefore(oldTotalBalance);
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
        
        log.info("付费套餐积分发放成功 - userId: {}, 发放积分: {}, 新余额: {}", 
                userId, credits, newTotalBalance);
                
        // 返回最新的积分信息
        return getUserCredit(userId);
    }

    /**
     * 转换积分交易记录为响应对象
     */
    private CreditTransactionResponse convertToResponse(CreditTransactionEntity entity) {
        CreditTransactionResponse response = BeanUtil.copyProperties(entity, CreditTransactionResponse.class);
        
        // 添加交易类型描述
        if (entity.getTransactionType() != null) {
            response.setTransactionTypeDesc(entity.getTransactionType().getDesc());
            
            // 设置收入支出标识
            response.setChangeType(entity.getTransactionType().isIncome() ? "+" : "-");
        }
        
        // 添加积分类型描述
        if (entity.getCreditType() != null) {
            response.setCreditTypeDesc(entity.getCreditType().getDesc());
        }
        
        return response;
    }
}
