package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.CreditTransactionResponse;

/**
 * 用户积分服务接口
 *
 * @author Noah
 * @since 1.0.0
 */
public interface UserCreditService {

    /**
     * 查询用户积分账户信息
     *
     * @param userId 用户ID
     * @return 用户积分账户信息
     */
    UserCreditResponse getUserCredit(Long userId);

    /**
     * 分页查询用户积分交易记录
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页的积分交易记录
     */
    PageResponse<CreditTransactionResponse> getCreditTransactions(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 初始化用户积分账户
     * （新用户注册时调用）
     *
     * @param userId 用户ID
     * @return 初始化后的积分账户信息
     */
    UserCreditResponse initUserCredit(Long userId);

    /**
     * 检查用户是否有积分账户
     *
     * @param userId 用户ID
     * @return 是否存在积分账户
     */
    boolean hasUserCredit(Long userId);

    /**
     * 获取用户当前可用积分总额
     *
     * @param userId 用户ID
     * @return 可用积分总额
     */
    Long getAvailableCredits(Long userId);

    /**
     * 给用户分配免费体验套餐并初始化积分账户
     * （用户注册成功后调用）
     *
     * @param userId 用户ID
     * @return 初始化后的积分账户信息
     */
    UserCreditResponse initFreePlanForUser(Long userId);

    /**
     * 免费套餐每日积分赠送
     * （定时任务调用）
     *
     * @param userId 用户ID
     * @return 赠送后的积分账户信息
     */
    UserCreditResponse giveFreePlanDailyBonus(Long userId);

    /**
     * 批量处理所有免费套餐用户的每日积分赠送
     * （定时任务调用，处理所有免费套餐用户）
     *
     * @return 处理结果统计
     */
    String processFreePlanDailyBonusForAllUsers();
}
