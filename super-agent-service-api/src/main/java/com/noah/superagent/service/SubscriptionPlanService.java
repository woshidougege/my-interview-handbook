package com.noah.superagent.service;

import com.noah.superagent.model.SubscriptionPlanDTO;

import java.util.List;

/**
 * 订阅套餐服务接口
 * 对应数据库表：t_subscription_plan
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface SubscriptionPlanService {

    /**
     * 获取所有启用的套餐列表
     *
     * @return 启用的套餐列表
     */
    List<SubscriptionPlanDTO> getEnabledPlans();
    
    /**
     * 根据ID获取套餐信息
     *
     * @param planId 套餐ID
     * @return 套餐信息
     */
    SubscriptionPlanDTO getPlanById(Long planId);

    /**
     * 获取带有当前套餐和可订阅状态的套餐列表
     * 用于前端展示套餐卡片时的状态控制
     *
     * @param currentUserPlanId 用户当前套餐ID（可为空）
     * @return 带有状态标识的套餐列表
     */
    List<SubscriptionPlanDTO> getEnabledPlansWithStatus(Long currentUserPlanId);
}