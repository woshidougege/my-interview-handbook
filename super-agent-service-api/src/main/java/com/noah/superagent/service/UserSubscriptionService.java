package com.noah.superagent.service;

import com.noah.superagent.model.UserSubscriptionDTO;

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
}