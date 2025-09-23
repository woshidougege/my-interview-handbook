package com.noah.superagent.service.impl;

import com.noah.superagent.common.config.PlansConfig;
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
 * @author 任相鹏
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
        
        // 价格信息将在calculatePriceFields中设置
        
        // 计算价格相关字段
        calculatePriceFields(dto, config);
        
        // 其他属性
        dto.setValidityDays(config.getValidityDays());
        dto.setDailyRefreshCredits(config.getDailyRefreshCredits());
        dto.setIsRecommended(config.getIsRecommended());
        dto.setSortOrder(config.getSortOrder());
        dto.setIsCurrentPlan(false); // 默认为false，在Controller层设置
        dto.setCreditsAmount(config.getCreditsAmount());
        
        // 转换功能特性
        dto.setFeatures(convertFeatures(config.getFeatures()));
        
        return dto;
    }

    @Override
    public List<SubscriptionPlanDTO> getEnabledPlansWithStatus(Long currentUserPlanId) {
        log.info("获取带状态的启用套餐列表 - currentUserPlanId: {}", currentUserPlanId);
        
        List<SubscriptionPlanDTO> planDTOs = getEnabledPlans();
        
        // 获取用户当前套餐的PlanCode（用于等级判断）
        PlanCodeEnum currentUserPlanCode = getCurrentUserPlanCode(planDTOs, currentUserPlanId);
        log.debug("用户当前套餐等级: {}", currentUserPlanCode != null ? currentUserPlanCode.getCode() : "无");
        
        // 设置套餐状态逻辑
        planDTOs.forEach(planDTO -> {
            // 判断套餐类型
            boolean isCreditPack = planDTO.getPlanCode() != null && planDTO.getPlanCode().isCreditPack();
            boolean isFreePlan = planDTO.getPlanCode() != null && planDTO.getPlanCode().isFree();
            
            // 设置是否为当前套餐：积分套餐永远显示false，其他套餐按用户当前订阅判断
            if (isCreditPack) {
                // 积分套餐永远不是"当前计划"
                planDTO.setIsCurrentPlan(false);
            } else {
                // 其他套餐（包括免费版）按正常逻辑判断
                planDTO.setIsCurrentPlan(currentUserPlanId != null && currentUserPlanId.equals(planDTO.getId()));
            }
            
            // 设置是否可订阅（加入套餐等级判断）
            if (planDTO.getIsCurrentPlan()) {
                // 当前套餐：不可订阅
                planDTO.setIsSubscribable(false);
            } else if (isCreditPack) {
                // 积分套餐：一直可以购买
                planDTO.setIsSubscribable(true);
            } else if (isFreePlan) {
                // 免费版：永远不能订阅（总是置灰）
                planDTO.setIsSubscribable(false);
            } else {
                // 其他付费套餐：根据套餐等级判断是否可以订阅
                boolean canSubscribe = canUpgradeToTarget(currentUserPlanCode, planDTO.getPlanCode());
                planDTO.setIsSubscribable(canSubscribe);
                
                log.debug("套餐升级判断: {} -> {}, 结果: {}", 
                    currentUserPlanCode != null ? currentUserPlanCode.getCode() : "无", 
                    planDTO.getPlanCode() != null ? planDTO.getPlanCode().getCode() : "无", 
                    canSubscribe);
            }

            if (planDTO.getPlanCode()==PlanCodeEnum.DEVELOPER_GOD) {
                        planDTO.setIsSubscribable(true);
            }
        });
        
        return planDTOs;
    }

    /**
     * 获取用户当前套餐的PlanCode
     */
    private PlanCodeEnum getCurrentUserPlanCode(List<SubscriptionPlanDTO> planDTOs, Long currentUserPlanId) {
        if (currentUserPlanId == null) {
            return null;
        }
        
        return planDTOs.stream()
                .filter(plan -> currentUserPlanId.equals(plan.getId()))
                .map(SubscriptionPlanDTO::getPlanCode)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否可以从当前套餐升级到目标套餐
     */
    private boolean canUpgradeToTarget(PlanCodeEnum currentPlan, PlanCodeEnum targetPlan) {
        // 如果用户没有当前套餐（比如新用户），则只能订阅付费套餐，不能订阅免费版
        if (currentPlan == null) {
            return targetPlan != null && !targetPlan.isFree() && !targetPlan.isCreditPack();
        }
        
        // 使用PlanCodeEnum中的升级逻辑
        return currentPlan.canUpgradeTo(targetPlan);
    }

    /**
     * 计算价格相关字段
     * 支持配置文件控制：-1表示自动计算，否则使用配置值
     */
    private void calculatePriceFields(SubscriptionPlanDTO dto, PlansConfig.PlanConfig config) {
        BigDecimal monthlyOriginalPrice = config.getMonthlyOriginalPrice();
        BigDecimal yearlyOriginalPrice = config.getYearlyOriginalPrice();
        
        // 全局优惠比例
        BigDecimal discountRate = BigDecimal.valueOf(plansConfig.getYearlyDiscountRate());
        
        if (monthlyOriginalPrice != null && monthlyOriginalPrice.compareTo(BigDecimal.ZERO) > 0) {
            
            // === 月价相关字段 ===
            
            // 设置月价原价（直接使用配置值）
            dto.setMonthlyOriginalPrice(monthlyOriginalPrice);
            
            // 月价优惠后价格
            BigDecimal monthlyDiscountedPrice = config.getMonthlyDiscountedPrice();
            if (monthlyDiscountedPrice.compareTo(BigDecimal.valueOf(-1)) == 0) {
                // 自动计算：月价应用17%优惠
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountRate);
                monthlyDiscountedPrice = monthlyOriginalPrice.multiply(discountMultiplier)
                    .setScale(0, RoundingMode.HALF_UP);
            }
            dto.setMonthlyPrice(monthlyDiscountedPrice);
            
            // 月价优惠金额
            BigDecimal monthlySavings = config.getMonthlySavings();
            if (monthlySavings.compareTo(BigDecimal.valueOf(-1)) == 0) {
                monthlySavings = monthlyOriginalPrice.subtract(monthlyDiscountedPrice);
            }
            dto.setMonthlySavings(monthlySavings);
            
            // === 年价相关字段 ===
            
            // 设置年价原价（如果未配置，则使用月价*12）
            if (yearlyOriginalPrice == null || yearlyOriginalPrice.compareTo(BigDecimal.ZERO) == 0) {
                yearlyOriginalPrice = monthlyOriginalPrice.multiply(new BigDecimal("12"));
            }
            dto.setYearlyOriginalPrice(yearlyOriginalPrice);
            
            // 年价优惠后价格
            BigDecimal yearlyDiscountedPrice = config.getYearlyDiscountedPrice();
            if (yearlyDiscountedPrice.compareTo(BigDecimal.valueOf(-1)) == 0) {
                // 自动计算：年价原价 * (1 - 优惠比例)
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountRate);
                yearlyDiscountedPrice = yearlyOriginalPrice.multiply(discountMultiplier)
                    .setScale(0, RoundingMode.HALF_UP);
            }
            dto.setYearlyPrice(yearlyDiscountedPrice);
            
            // 年价优惠金额
            BigDecimal yearlySavings = config.getYearlySavings();
            if (yearlySavings.compareTo(BigDecimal.valueOf(-1)) == 0) {
                yearlySavings = yearlyOriginalPrice.subtract(yearlyDiscountedPrice);
            }
            dto.setYearlySavings(yearlySavings);
            
        } else {
            // 免费套餐，所有价格字段都为0
            dto.setMonthlyOriginalPrice(BigDecimal.ZERO);
            dto.setMonthlyPrice(BigDecimal.ZERO);
            dto.setMonthlySavings(BigDecimal.ZERO);
            dto.setYearlyOriginalPrice(BigDecimal.ZERO);
            dto.setYearlyPrice(BigDecimal.ZERO);
            dto.setYearlySavings(BigDecimal.ZERO);
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
            // 先尝试按枚举常量名解析（如CREDIT_PACK）
            return PlanCodeEnum.valueOf(code);
        } catch (IllegalArgumentException e1) {
            try {
                // 再尝试按code值解析（如credit_pack）
                return PlanCodeEnum.getByCode(code);
            } catch (Exception e2) {
                log.warn("未知的套餐代码: {}", code);
                return null;
            }
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
                    return feature;
                })
                .collect(Collectors.toList());
    }
}
