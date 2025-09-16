package com.noah.superagent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
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
import com.noah.superagent.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /**
     * 免费套餐每日积分数量（从配置文件读取）
     */
    @Value("${super-agent.billing.credits.free-credits.daily-signin:300}")
    private Integer dailySigninCredits;
    
    /**
     * 新用户积分数量（从配置文件读取）
     */
    @Value("${super-agent.billing.credits.free-credits.new-user-amount:1000}")
    private Integer newUserCredits;
    
    // 用于防止重复赠送的日期格式
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取免费套餐每日积分数量
     */
    private BigDecimal getFreePlanDailyCredits() {
        return new BigDecimal(dailySigninCredits.toString());
    }
    
    /**
     * 获取新用户积分数量
     */
    private BigDecimal getNewUserCredits() {
        return new BigDecimal(newUserCredits.toString());
    }

    @Override
    public UserCreditResponse getUserCredit(Long userId) {
        log.info("查询用户积分账户信息 - userId: {}", userId);
        
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.warn("用户积分账户不存在 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
        }

        // 查询用户各类型积分余额
        List<UserCreditBalanceEntity> balanceList = userCreditBalanceMapper.selectByUserId(userId);
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
        response.setFreeBalance(balanceMap.getOrDefault(CreditTypeEnum.NEW_USER, BigDecimal.ZERO)); // 新用户积分作为免费积分
        response.setPermanentBalance(balanceMap.getOrDefault(CreditTypeEnum.PERMANENT, BigDecimal.ZERO));
        
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
    @Transactional(rollbackFor = Exception.class)
    public UserCreditResponse initUserCredit(Long userId) {
        log.info("初始化用户积分账户 - userId: {}", userId);
        
        // 检查是否已存在积分账户
        UserCreditAccountEntity existingAccount = userCreditAccountMapper.selectByUserId(userId);
        if (existingAccount != null) {
            log.warn("用户积分账户已存在 - userId: {}", userId);
            return getUserCredit(userId);
        }
        
        // 创建新的积分账户（仅创建汇总表，不创建明细表）
        UserCreditAccountEntity creditAccount = new UserCreditAccountEntity();
        creditAccount.setUserId(userId);
        creditAccount.setTotalBalance(BigDecimal.ZERO);
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
        // 初始化时所有类型积分余额都为0
        response.setDailyBalance(BigDecimal.ZERO);
        response.setActivityBalance(BigDecimal.ZERO);
        response.setFreeBalance(BigDecimal.ZERO);
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
        newUserBalance.setCreditType(CreditTypeEnum.NEW_USER);
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
        log.info("创建新用户积分余额记录成功 - userId: {}, type: {}, balance: {}", userId, CreditTypeEnum.NEW_USER, getNewUserCredits());
        
        // 3. 记录新用户赠送积分的交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY); // TODO: 需要新增新用户赠送类型
        transaction.setCreditType(CreditTypeEnum.NEW_USER);
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
    public UserCreditResponse giveFreePlanDailyBonusOnLogin(Long userId) {
        log.info("用户登录时检查并发放免费套餐每日积分 - userId: {}, 积分数量: {}", userId, getFreePlanDailyCredits());
        
        // 1. 查询用户积分账户，如果不存在则先初始化
        UserCreditAccountEntity creditAccount = userCreditAccountMapper.selectByUserId(userId);
        if (creditAccount == null) {
            log.info("用户积分账户不存在，先初始化账户 - userId: {}", userId);
            // 先初始化免费套餐账户
            initFreePlanForUser(userId);
            // 重新查询积分账户
            creditAccount = userCreditAccountMapper.selectByUserId(userId);
            if (creditAccount == null) {
                log.error("初始化积分账户后仍然为空 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
            }
            log.info("积分账户初始化完成，当前余额: {} - userId: {}", creditAccount.getTotalBalance(), userId);
        }
        
        // 2. 检查今日是否已经发放过积分（防重）
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        
        List<CreditTransactionEntity> todayTransactions = creditTransactionMapper.selectByUserIdAndType(
            userId, CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY);
        
        boolean alreadyGivenToday = todayTransactions.stream()
                .anyMatch(transaction -> transaction.getDescription() != null && 
                         transaction.getDescription().contains(today));
        
        if (alreadyGivenToday) {
            log.info("用户今日已经发放过免费套餐积分 - userId: {}", userId);
            return getUserCredit(userId);
        }
        
        // 3. 更新积分汇总账户
        BigDecimal oldTotalBalance = creditAccount.getTotalBalance();
        BigDecimal newTotalBalance = oldTotalBalance.add(getFreePlanDailyCredits());
        BigDecimal newTotalEarned = creditAccount.getTotalEarned().add(getFreePlanDailyCredits());
        
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
        
        // 4. 创建或更新每日积分余额记录
        UserCreditBalanceEntity dailyBalance = userCreditBalanceMapper.selectByUserIdAndCreditType(userId, CreditTypeEnum.DAILY);
        BigDecimal newDailyBalance = getFreePlanDailyCredits();
        
        if (dailyBalance == null) {
            // 创建新的每日积分记录
            dailyBalance = new UserCreditBalanceEntity();
            dailyBalance.setUserId(userId);
            dailyBalance.setCreditType(CreditTypeEnum.DAILY);
            dailyBalance.setBalance(newDailyBalance);
            dailyBalance.setTotalEarned(getFreePlanDailyCredits());
            dailyBalance.setTotalSpent(BigDecimal.ZERO);
            dailyBalance.setLastEarnTime(LocalDateTime.now());
            dailyBalance.setVersion(0);
            dailyBalance.setCreateBy(userId);
            
            int dailyBalanceResult = userCreditBalanceMapper.insertOrUpdate(dailyBalance);
            if (dailyBalanceResult <= 0) {
                log.error("创建用户每日积分余额记录失败 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "创建每日积分余额记录失败");
            }
            log.info("创建每日积分记录成功 - userId: {}, 积分: {}", userId, getFreePlanDailyCredits());
        } else {
            // 更新现有的每日积分记录（直接替换，因为每日积分只保留当天的）
            dailyBalance.setBalance(newDailyBalance); // 每日积分覆盖模式
            dailyBalance.setTotalEarned(dailyBalance.getTotalEarned().add(getFreePlanDailyCredits()));
            dailyBalance.setLastEarnTime(LocalDateTime.now());
            dailyBalance.setVersion(dailyBalance.getVersion());
            dailyBalance.setUpdateBy(userId);
            
            int balanceUpdateResult = userCreditBalanceMapper.updateBalanceByUserIdAndCreditType(
                userId, CreditTypeEnum.DAILY, dailyBalance);
            if (balanceUpdateResult <= 0) {
                log.error("更新用户每日积分余额失败 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新每日积分余额失败");
            }
        }
        
        // 5. 记录积分交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_FREE_PLAN_DAILY);
        transaction.setCreditType(CreditTypeEnum.DAILY);
        transaction.setAmount(getFreePlanDailyCredits());
        transaction.setBalanceBefore(oldTotalBalance);
        transaction.setBalanceAfter(newTotalBalance);
        transaction.setDescription("每日登录赠送积分（24小时有效） - " + today);
        transaction.setExpireTime(LocalDateTime.now().plusDays(1)); // 1天后过期
        transaction.setCreateBy(userId);
        
        int transactionResult = creditTransactionMapper.insertTransaction(transaction);
        if (transactionResult <= 0) {
            log.error("插入积分交易记录失败 - userId: {}", userId);
            throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "记录交易失败");
        }
        
        log.info("免费套餐每日积分发放成功 - userId: {}, 发放积分: {}, 新余额: {}", 
                userId, getFreePlanDailyCredits(), newTotalBalance);
                
        // 返回最新的积分信息
        return getUserCredit(userId);
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
        UserCreditBalanceEntity permanentBalance = userCreditBalanceMapper.selectByUserIdAndCreditType(userId, CreditTypeEnum.PERMANENT);
        
        if (permanentBalance == null) {
            // 创建新的永久积分记录
            permanentBalance = new UserCreditBalanceEntity();
            permanentBalance.setUserId(userId);
            permanentBalance.setCreditType(CreditTypeEnum.PERMANENT);
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
                userId, CreditTypeEnum.PERMANENT, permanentBalance);
            if (balanceUpdateResult <= 0) {
                log.error("更新用户永久积分余额失败 - userId: {}", userId);
                throw new BusinessException(ResponseCodeEnum.DATABASE_ERROR, "更新永久积分余额失败");
            }
        }
        
        // 4. 记录积分交易记录
        CreditTransactionEntity transaction = new CreditTransactionEntity();
        transaction.setUserId(userId);
        transaction.setTransactionType(CreditTransactionTypeEnum.INCOME_PRO_PLAN); // 使用PRO套餐类型代表付费积分
        transaction.setCreditType(CreditTypeEnum.PERMANENT);
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
