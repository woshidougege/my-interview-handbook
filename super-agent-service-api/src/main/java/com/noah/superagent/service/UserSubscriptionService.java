package com.noah.superagent.service;

import com.noah.superagent.model.UserSubscriptionDTO;
import java.util.List;

/**
 * 用户订阅服务接口
 * 对应数据库表：t_user_subscription
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public interface UserSubscriptionService {

    /**
     * 获取用户当前有效订阅
     *
     * @param userId 用户ID
     * @return 当前有效订阅，如果没有返回null
     */
    UserSubscriptionDTO getCurrentActiveSubscription(Long userId);
    
    /**
     * 获取用户所有有效订阅（按等级排序）
     *
     * @param userId 用户ID
     * @return 有效订阅列表，按套餐等级降序排列
     */
    List<UserSubscriptionDTO> getAllActiveSubscriptions(Long userId);
    
    /**
     * 智能激活用户订阅
     * 支持升级时延长下级套餐有效期的逻辑
     *
     * @param userId 用户ID
     * @param planId 套餐ID
     * @param billingCycle 计费周期(monthly/yearly)
     * @param orderNo 订单号
     * @param paidAmount 支付金额
     * @return 激活后的订阅记录
     */
    UserSubscriptionDTO activateSubscriptionWithUpgradeLogic(Long userId, Long planId, String billingCycle, 
                                                            String orderNo, java.math.BigDecimal paidAmount);
    
    /**
     * 处理套餐到期和降级逻辑
     * 定时任务调用，处理高级套餐到期后自动回到基础套餐
     *
     * @return 处理的订阅数量
     */
    int processSubscriptionExpirations();
    
    /**
     * 处理特定订阅到期
     * 由 db-scheduler 的 OneTimeTask 调用
     *
     * @param subscriptionId 订阅ID
     * @return 是否处理成功
     */
    boolean processSpecificSubscriptionExpiration(Long subscriptionId);
    
}