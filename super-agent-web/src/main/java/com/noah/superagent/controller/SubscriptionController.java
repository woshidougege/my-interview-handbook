package com.noah.superagent.controller;

import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import com.noah.superagent.dao.mapper.SubscriptionPlanMapper;
import com.noah.superagent.dao.mapper.UserSubscriptionMapper;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.common.enums.EnabledEnum;
import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    private final SubscriptionPlanMapper subscriptionPlanMapper;
    private final UserSubscriptionMapper userSubscriptionMapper;

    @Operation(summary = "获取套餐列表", description = "获取所有启用的套餐")
    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlanEntity>> getPlans() {
        List<SubscriptionPlanEntity> plans = subscriptionPlanMapper.selectAll()
            .stream()
            .filter(plan -> EnabledEnum.ENABLED.equals(plan.getEnabled()))
            .collect(Collectors.toList());
        
        return ApiResponse.success("获取套餐列表成功", plans);
    }

    @Operation(summary = "获取用户当前订阅", description = "获取用户当前有效订阅")
    @GetMapping("/current")
    public ApiResponse<UserSubscriptionEntity> getCurrentSubscription(
            @Parameter(description = "用户ID") @RequestParam @NotNull Long userId) {
        
        // 查询用户当前有效订阅
        UserSubscriptionEntity subscription = userSubscriptionMapper.selectAll()
            .stream()
            .filter(sub -> sub.getUserId().equals(userId) 
                && SubscriptionStatusEnum.ACTIVE.equals(sub.getStatus())
                && sub.getEndTime().isAfter(LocalDateTime.now()))
            .findFirst()
            .orElse(null);
        
        return ApiResponse.success("获取当前订阅成功", subscription);
    }
}
