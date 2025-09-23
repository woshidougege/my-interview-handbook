package com.noah.superagent.service;

import com.noah.superagent.model.SubscriptionPlanDTO;

import java.util.List;

/**
 * 套餐模板服务接口
 * 管理系统预设的套餐类型（免费版、基础版、高级版等）
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface PlanService {

    /**
     * 获取所有启用的套餐列表
     *
     * @return 启用的套餐列表
     */
    List<SubscriptionPlanDTO> getEnabledPlans();
}
