package com.noah.superagent.service;

import com.noah.superagent.model.SubscriptionPlanDTO;
import com.noah.superagent.model.UserSubscriptionDTO;

import java.util.List;

/**
 * 订阅服务接口
 * 管理套餐模板和用户订阅记录
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface SubscriptionService {

    // ========== 套餐模板管理 ==========
    
    /**
     * 获取所有启用的套餐列表
     *
     * @return 启用的套餐列表
     */
    List<SubscriptionPlanDTO> getEnabledPlans();

    // ========== 用户订阅管理 ==========
    
    /**
     * 获取用户当前有效订阅
     *
     * @param userId 用户ID
     * @return 当前有效订阅，如果没有返回null
     */
    UserSubscriptionDTO getCurrentActiveSubscription(Long userId);
}
