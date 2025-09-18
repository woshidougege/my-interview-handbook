package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.CreditTransactionResponse;

import java.math.BigDecimal;

/**
 * 用户积分服务接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface UserCreditService {

    /**
     * 检查用户是否可以消费指定积分（包含透支检查）
     *
     * @param userId 用户ID
     * @param amount 需要的积分数量
     * @return 是否可以消费
     */
    boolean canConsumeCredits(Long userId, BigDecimal amount);

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
     * 检查用户是否有积分账户
     *
     * @param userId 用户ID
     * @return 是否存在积分账户
     */
    boolean hasUserCredit(Long userId);

    /**
     * 给用户分配免费体验套餐并初始化积分账户
     * （用户注册成功后调用）
     *
     * @param userId 用户ID
     * @return 初始化后的积分账户信息
     */
    UserCreditResponse initFreePlanForUser(Long userId);

    /**
     * 处理用户每日登录活动
     * <p>
     * 此方法会处理两件事：
     * 1. 记录用户的每日登录（首次或更新次数）。
     * 2. 检查并根据用户的套餐发放每日积分（如果当天尚未发放）。
     *
     * @param userId 用户ID
     */
    void handleUserLogin(Long userId);

    /**
     * 为用户发放付费套餐的永久积分
     *
     * @param userId 用户ID
     * @param creditAmount 积分数量
     * @param orderId 订单ID
     * @param planName 套餐名称
     * @return 积分账户信息
     */
    UserCreditResponse grantPaidPlanCredits(Long userId, Long creditAmount, Long orderId, String planName);

}
