package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.SubscriptionPlanResponse;
import com.noah.superagent.convert.SubscriptionPlanWebConvert;
import com.noah.superagent.model.UserSubscriptionDTO;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.response.UserSubscriptionWithCreditResponse;
import com.noah.superagent.service.SubscriptionPlanService;
import com.noah.superagent.service.UserSubscriptionService;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简化的订阅管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
@Validated
@Tag(name = "订阅管理", description = "简化的订阅相关接口")
public class SubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPlanWebConvert webConvert;
    private final UserSubscriptionService userSubscriptionService;
    private final UserCreditService userCreditService;

    @Operation(summary = "获取套餐列表", description = "获取所有启用的套餐")
    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlanResponse>> getPlans() {
        List<SubscriptionPlanResponse> plans = webConvert.toResponseList(
            subscriptionPlanService.getEnabledPlans()
        );
        
        return ApiResponse.success("获取套餐列表成功", plans);
    }

    @Operation(summary = "获取用户当前订阅和积分信息", description = "获取当前登录用户的有效订阅信息以及可用积分总数")
    @GetMapping("/current")
    public ApiResponse<UserSubscriptionWithCreditResponse> getCurrentSubscriptionWithCredit() {
        
        Long userId = UserContext.requireCurrentUserId();
        log.info("查询用户当前订阅和积分信息 - userId: {}", userId);
        
        try {
            // 查询用户当前有效订阅
            UserSubscriptionDTO subscription = userSubscriptionService.getCurrentActiveSubscription(userId);
            
            // 查询用户可用积分
            Long availableCredits = 0L;
            boolean hasCreditAccount = false;
            
            try {
                hasCreditAccount = userCreditService.hasUserCredit(userId);
                if (hasCreditAccount) {
                    availableCredits = userCreditService.getAvailableCredits(userId);
                }
            } catch (Exception e) {
                log.warn("查询用户积分信息失败 - userId: {}, 错误: {}", userId, e.getMessage());
                // 积分查询失败不影响订阅信息返回
            }
            
            // 构建响应
            UserSubscriptionWithCreditResponse response = new UserSubscriptionWithCreditResponse();
            response.setSubscription(subscription);
            response.setAvailableCredits(availableCredits);
            response.setHasCreditAccount(hasCreditAccount);
            
            return ApiResponse.success("获取订阅和积分信息成功", response);
            
        } catch (Exception e) {
            log.error("查询用户订阅和积分信息失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}
