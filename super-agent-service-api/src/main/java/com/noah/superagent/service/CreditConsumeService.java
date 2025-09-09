package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.UserCreditResponse;

import java.math.BigDecimal;

/**
 * 积分消费服务接口
 * 
 * 处理积分扣费逻辑，按照有效期优先级顺序扣费
 *
 * @author Noah
 * @since 1.0.0
 */
public interface CreditConsumeService {

    /**
     * 消费积分（按优先级顺序扣费）
     * 
     * 扣费顺序：当日积分 → 活动积分 → 免费积分 → 永久积分
     *
     * @param userId 用户ID
     * @param amount 消费积分数量
     * @param description 消费描述
     * @param relatedOrderId 关联订单ID（可选）
     * @return 消费后的积分账户信息
     */
    UserCreditResponse consumeCredits(Long userId, BigDecimal amount, String description, Long relatedOrderId);

    /**
     * 检查用户是否有足够积分
     *
     * @param userId 用户ID
     * @param amount 需要的积分数量
     * @return 是否有足够积分
     */
    boolean hasEnoughCredits(Long userId, BigDecimal amount);

    /**
     * 预览积分消费（不实际扣费，仅返回扣费计划）
     *
     * @param userId 用户ID
     * @param amount 消费积分数量
     * @return 积分扣费计划详情
     */
    String previewCreditConsumption(Long userId, BigDecimal amount);
}
