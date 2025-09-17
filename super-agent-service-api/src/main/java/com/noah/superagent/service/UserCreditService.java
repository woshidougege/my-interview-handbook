package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.UserCreditStatusResponse;
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
     * 获取用户积分详细状态
     * （包含总积分、各类型积分、透支额度、账户状态等完整信息）
     *
     * @param userId 用户ID
     * @return 积分详细状态信息
     */
    UserCreditStatusResponse getUserCreditStatus(Long userId);

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
     * 用户登录时检查并发放每日积分
     * （根据用户当前套餐配置发放对应积分数量，一天只发放一次）
     * 免费版：300积分/天，基础版：1900积分/天，高级版：5900积分/天
     *
     * @param userId 用户ID
     * @return 积分账户信息
     */
    UserCreditResponse giveFreePlanDailyBonusOnLogin(Long userId);

    
    /**
     * 发放付费套餐永久积分
     * （支付成功后调用）
     *
     * @param userId 用户ID
     * @param creditAmount 积分数量
     * @param orderId 订单ID
     * @param planName 套餐名称
     * @return 积分账户信息
     */
    UserCreditResponse grantPaidPlanCredits(Long userId, Long creditAmount, Long orderId, String planName);
}
