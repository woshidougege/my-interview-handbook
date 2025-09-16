package com.noah.superagent.service.impl;

import com.noah.superagent.common.config.PlansConfig;
import com.noah.superagent.common.enums.BillingCycleEnum;
import com.noah.superagent.common.enums.PlanCodeEnum;
import com.noah.superagent.model.PlanFeatureDTO;
import com.noah.superagent.model.SubscriptionPlanDTO;
import com.noah.superagent.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于配置文件的订阅套餐服务实现类
 * 替代数据库查询，从配置文件读取套餐信息
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigBasedSubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final PlansConfig plansConfig;

    @Override
    public List<SubscriptionPlanDTO> getEnabledPlans() {
        log.info("从配置文件获取启用的订阅套餐列表");
        
        return plansConfig.getSubscriptionPlans()
                .stream()
                .filter(plan -> plan.getEnabled() != null && plan.getEnabled())
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(SubscriptionPlanDTO::getSortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionPlanDTO getPlanById(Long planId) {
        log.info("根据ID获取套餐信息 - planId: {}", planId);
        
        return plansConfig.getSubscriptionPlans()
                .stream()
                .filter(plan -> planId.toString().equals(plan.getId()))
                .map(this::convertToDTO)
                .findFirst()
                .orElse(null);
    }

    /**
     * 将配置转换为DTO
     */
    private SubscriptionPlanDTO convertToDTO(PlansConfig.PlanConfig config) {
        SubscriptionPlanDTO dto = new SubscriptionPlanDTO();
        
        // 基础信息
        dto.setId(Long.parseLong(config.getId()));
        dto.setPlanName(config.getName());
        dto.setPlanCode(parsePlanCode(config.getCode()));
        dto.setDescription(config.getDescription());
        
        // 价格信息
        dto.setMonthlyPrice(config.getMonthlyPrice());
        dto.setYearlyPrice(config.getYearlyPrice());
        
        // 计算价格相关字段
        calculatePriceFields(dto, config);
        
        // 其他属性
        dto.setValidityDays(config.getValidityDays());
        dto.setIsRecommended(config.getIsRecommended());
        dto.setSortOrder(config.getSortOrder());
        dto.setIsCurrentPlan(false); // 默认为false，在Controller层设置
        
        // 转换功能特性
        dto.setFeatures(convertFeatures(config.getFeatures()));
        
        return dto;
    }

    /**
     * 计算价格相关字段
     * 支持配置文件控制：-1表示自动计算，否则使用配置值
     */
    private void calculatePriceFields(SubscriptionPlanDTO dto, PlansConfig.PlanConfig config) {
        BigDecimal monthlyPrice = config.getMonthlyPrice();
        BigDecimal yearlyPrice = config.getYearlyPrice();
        
        // 全局优惠比例
        BigDecimal discountRate = BigDecimal.valueOf(plansConfig.getYearlyDiscountRate());
        
        if (monthlyPrice != null && monthlyPrice.compareTo(BigDecimal.ZERO) > 0) {
            
            // === 月价相关字段 ===
            
            // 月价原价
            BigDecimal monthlyOriginalPrice = config.getMonthlyOriginalPrice();
            if (monthlyOriginalPrice.compareTo(BigDecimal.valueOf(-1)) == 0) {
                monthlyOriginalPrice = monthlyPrice; // 月价原价等于月价
            }
            dto.setMonthlyOriginalPrice(monthlyOriginalPrice);
            
            // 月价优惠后价格
            BigDecimal monthlyDiscountedPrice = config.getMonthlyDiscountedPrice();
            if (monthlyDiscountedPrice.compareTo(BigDecimal.valueOf(-1)) == 0) {
                // 月价目前没有优惠，优惠后价格等于原价
                monthlyDiscountedPrice = monthlyPrice;
            }
            dto.setMonthlyPrice(monthlyDiscountedPrice);
            
            // 月价优惠金额
            BigDecimal monthlySavings = config.getMonthlySavings();
            if (monthlySavings.compareTo(BigDecimal.valueOf(-1)) == 0) {
                monthlySavings = monthlyOriginalPrice.subtract(monthlyDiscountedPrice);
            }
            dto.setMonthlySavings(monthlySavings);
            
            // === 年价相关字段 ===
            
            // 年价原价
            BigDecimal yearlyOriginalPrice = config.getYearlyOriginalPrice();
            if (yearlyOriginalPrice.compareTo(BigDecimal.valueOf(-1)) == 0) {
                yearlyOriginalPrice = monthlyPrice.multiply(new BigDecimal("12"));
            }
            dto.setYearlyOriginalPrice(yearlyOriginalPrice);
            
            // 年价优惠后价格
            BigDecimal yearlyDiscountedPrice = config.getYearlyDiscountedPrice();
            if (yearlyDiscountedPrice.compareTo(BigDecimal.valueOf(-1)) == 0) {
                if (yearlyPrice != null && yearlyPrice.compareTo(BigDecimal.ZERO) > 0) {
                    yearlyDiscountedPrice = yearlyPrice; // 使用配置的年价
                } else {
                    // 自动计算：年价原价 * (1 - 优惠比例)
                    BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountRate);
                    yearlyDiscountedPrice = yearlyOriginalPrice.multiply(discountMultiplier)
                        .setScale(0, RoundingMode.HALF_UP);
                }
            }
            dto.setYearlyPrice(yearlyDiscountedPrice);
            
            // 年价优惠金额
            BigDecimal yearlySavings = config.getYearlySavings();
            if (yearlySavings.compareTo(BigDecimal.valueOf(-1)) == 0) {
                yearlySavings = yearlyOriginalPrice.subtract(yearlyDiscountedPrice);
            }
            dto.setYearlySavings(yearlySavings);
            
            // 年价月均优惠金额
            dto.setYearlyMonthlySavings(yearlySavings.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP));
            
        } else {
            // 免费套餐，所有价格字段都为0
            dto.setMonthlyOriginalPrice(BigDecimal.ZERO);
            dto.setMonthlyPrice(BigDecimal.ZERO);
            dto.setMonthlySavings(BigDecimal.ZERO);
            dto.setYearlyOriginalPrice(BigDecimal.ZERO);
            dto.setYearlyPrice(BigDecimal.ZERO);
            dto.setYearlySavings(BigDecimal.ZERO);
            dto.setYearlyMonthlySavings(BigDecimal.ZERO);
        }
    }

    /**
     * 解析套餐代码
     */
    private PlanCodeEnum parsePlanCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return PlanCodeEnum.valueOf(code);
        } catch (IllegalArgumentException e) {
            log.warn("未知的套餐代码: {}", code);
            return null;
        }
    }

    /**
     * 转换功能特性
     */
    private List<PlanFeatureDTO> convertFeatures(List<PlansConfig.FeatureConfig> featureConfigs) {
        if (featureConfigs == null) {
            return List.of();
        }
        
        return featureConfigs.stream()
                .map(config -> {
                    PlanFeatureDTO feature = new PlanFeatureDTO();
                    feature.setText(config.getText());
                    feature.setHighlight(config.getHighlight());
                    feature.setIncluded(config.getIncluded());
                    return feature;
                })
                .collect(Collectors.toList());
    }
}
