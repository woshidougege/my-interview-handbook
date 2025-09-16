package com.noah.superagent.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.noah.superagent.common.enums.BillingCycleEnum;
import com.noah.superagent.common.enums.EnabledEnum;
import com.noah.superagent.convert.SubscriptionPlanPersistenceConvert;
import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import com.noah.superagent.dao.mapper.SubscriptionPlanMapper;
import com.noah.superagent.model.SubscriptionPlanDTO;
import com.noah.superagent.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订阅套餐服务实现类
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl extends ServiceImpl<SubscriptionPlanMapper,SubscriptionPlanEntity> implements SubscriptionPlanService {

    private final SubscriptionPlanMapper subscriptionPlanMapper;
    private final SubscriptionPlanPersistenceConvert convert;

    @Override
    public List<SubscriptionPlanDTO> getEnabledPlans() {
        log.info("获取启用的订阅套餐列表");
        
        return subscriptionPlanMapper.selectAll()
                .stream()
                .filter(plan -> EnabledEnum.ENABLED.equals(plan.getEnabled()))
                .map(convert::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionPlanDTO> getEnabledPlansByBillingCycle(BillingCycleEnum billingCycle) {
        log.info("获取启用的订阅套餐列表 - billingCycle: {}", billingCycle);
        
        List<SubscriptionPlanDTO> plans = getEnabledPlans();
        
        // 如果是按年计费，需要调整价格（优惠17%）
        if (BillingCycleEnum.YEARLY.equals(billingCycle)) {
            return plans.stream()
                    .map(this::adjustPriceForYearly)
                    .collect(Collectors.toList());
        }
        
        return plans;
    }

    /**
     * 为按年计费调整价格（优惠17%）
     */
    private SubscriptionPlanDTO adjustPriceForYearly(SubscriptionPlanDTO plan) {
        if (plan.getMonthlyPrice() != null && plan.getMonthlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            // 按年价格 = 月价格 * 12 * 0.83 (优惠17%)
            BigDecimal yearlyPrice = plan.getMonthlyPrice()
                    .multiply(BigDecimal.valueOf(12))
                    .multiply(BigDecimal.valueOf(0.83))
                    .setScale(0, RoundingMode.HALF_UP);
            
            plan.setYearlyPrice(yearlyPrice);
            // 兼容字段也设置为按年价格
            plan.setPrice(yearlyPrice);
        }
        
        return plan;
    }
    
    @Override
    public SubscriptionPlanDTO getPlanById(Long planId) {
        log.info("根据ID获取套餐信息 - planId: {}", planId);
        
        if (planId == null) {
            return null;
        }
        
        var entity = this.getById(planId);
        return entity != null ? convert.fromEntity(entity) : null;
    }
}
