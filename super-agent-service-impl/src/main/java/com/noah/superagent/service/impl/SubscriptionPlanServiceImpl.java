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
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * 按年订阅优惠比例（从配置文件读取，默认17%）
     */
    @Value("${super-agent.billing.subscription.yearly-discount-rate:0.17}")
    private Double yearlyDiscountRate;

    @Override
    public List<SubscriptionPlanDTO> getEnabledPlans() {
        log.info("获取启用的订阅套餐列表");
        
        return subscriptionPlanMapper.selectAllOrderById()
                .stream()
                .filter(plan -> EnabledEnum.ENABLED.equals(plan.getEnabled()))
                .map(convert::fromEntity)
                .map(this::setDiscountRate)
                .collect(Collectors.toList());
    }

    /**
     * 设置优惠比例和优惠金额（用于按月查询时也提供完整优惠信息）
     */
    private SubscriptionPlanDTO setDiscountRate(SubscriptionPlanDTO plan) {
        // 判断是否为积分套餐
        boolean isCreditsOnly = plan.getPlanName() != null && plan.getPlanName().contains("积分");
        
        if (!isCreditsOnly && plan.getMonthlyPrice() != null && plan.getMonthlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            plan.setYearlyDiscountRate(yearlyDiscountRate);
            
            // 计算优惠金额
            BigDecimal originalYearlyPrice = plan.getMonthlyPrice().multiply(BigDecimal.valueOf(12));
            BigDecimal totalSavings = originalYearlyPrice.multiply(BigDecimal.valueOf(yearlyDiscountRate));
            BigDecimal monthlySavings = totalSavings.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            
            plan.setYearlyTotalSavings(totalSavings.setScale(0, RoundingMode.HALF_UP));
            plan.setYearlyMonthlySavings(monthlySavings);
        } else {
            plan.setYearlyDiscountRate(0.0);
            plan.setYearlyTotalSavings(BigDecimal.ZERO);
            plan.setYearlyMonthlySavings(BigDecimal.ZERO);
        }
        
        return plan;
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
     * 为按年计费调整价格和计算优惠金额
     * 只对非积分套餐进行优惠
     */
    private SubscriptionPlanDTO adjustPriceForYearly(SubscriptionPlanDTO plan) {
        // 判断是否为积分套餐（单独购买积分），如果是则不优惠
        boolean isCreditsOnly = plan.getPlanName() != null && plan.getPlanName().contains("积分");
        
        if (!isCreditsOnly && plan.getMonthlyPrice() != null && plan.getMonthlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            // 原年价格（无优惠）
            BigDecimal originalYearlyPrice = plan.getMonthlyPrice().multiply(BigDecimal.valueOf(12));
            
            // 按年价格 = 月价格 * 12 * (1 - 优惠比例)
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(BigDecimal.valueOf(yearlyDiscountRate));
            BigDecimal yearlyPrice = originalYearlyPrice
                    .multiply(discountMultiplier)
                    .setScale(0, RoundingMode.HALF_UP);
            
            // 计算优惠金额
            BigDecimal totalSavings = originalYearlyPrice.subtract(yearlyPrice);
            BigDecimal monthlySavings = totalSavings.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            
            plan.setYearlyPrice(yearlyPrice);
            plan.setPrice(yearlyPrice); // 兼容字段也设置为按年价格
            plan.setYearlyDiscountRate(yearlyDiscountRate);
            plan.setYearlyTotalSavings(totalSavings);
            plan.setYearlyMonthlySavings(monthlySavings);
        } else {
            // 积分套餐不优惠
            plan.setYearlyDiscountRate(0.0);
            plan.setYearlyTotalSavings(BigDecimal.ZERO);
            plan.setYearlyMonthlySavings(BigDecimal.ZERO);
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
