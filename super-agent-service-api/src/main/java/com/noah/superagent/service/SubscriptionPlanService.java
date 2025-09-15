package com.noah.superagent.service;

import com.noah.superagent.model.SubscriptionPlanDTO;

import java.util.List;

/**
 * 订阅套餐服务接口
 * 对应数据库表：t_subscription_plan
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public interface SubscriptionPlanService {

    /**
     * 获取所有启用的套餐列表
     *
     * @return 启用的套餐列表
     */
    List<SubscriptionPlanDTO> getEnabledPlans();
}