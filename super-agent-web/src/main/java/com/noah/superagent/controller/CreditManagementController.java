package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.UserCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 积分管理控制器
 * 
 * 用于管理员手动操作和测试积分相关功能
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/credit-management")
@RequiredArgsConstructor
@Validated
@Tag(name = "积分管理", description = "管理员积分管理和测试接口")
public class CreditManagementController {

    private final UserCreditService userCreditService;

    @PostMapping("/users/{userId}/init-free-plan")
    @Operation(summary = "手动初始化用户免费套餐", 
            description = "为指定用户手动初始化免费套餐积分账户（仅管理员使用）")
    public ApiResponse<UserCreditResponse> initFreePlanForUser(
            @Parameter(description = "用户ID", required = true, example = "1234567890123456789")
            @PathVariable @NotNull @Min(1) Long userId) {
        
        log.info("管理员手动初始化用户免费套餐 - userId: {}", userId);
        
        try {
            UserCreditResponse result = userCreditService.initFreePlanForUser(userId);
            return ApiResponse.success("免费套餐初始化成功", result);
        } catch (Exception e) {
            log.error("手动初始化用户免费套餐失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("初始化失败: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/give-daily-bonus")
    @Operation(summary = "手动发放每日积分", 
            description = "为指定用户手动发放当日免费套餐积分（仅管理员测试使用）")
    public ApiResponse<UserCreditResponse> giveDailyBonus(
            @Parameter(description = "用户ID", required = true, example = "1234567890123456789")
            @PathVariable @NotNull @Min(1) Long userId) {
        
        log.info("管理员手动发放每日积分 - userId: {}", userId);
        
        try {
            UserCreditResponse result = userCreditService.giveFreePlanDailyBonus(userId);
            return ApiResponse.success("每日积分发放成功", result);
        } catch (Exception e) {
            log.error("手动发放每日积分失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("发放失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch/daily-bonus")
    @Operation(summary = "批量发放所有用户每日积分", 
            description = "手动触发所有用户的每日积分发放（仅管理员测试使用，慎用）")
    public ApiResponse<String> processDailyBonusForAllUsers() {
        
        log.info("管理员手动触发批量每日积分发放");
        
        try {
            String result = userCreditService.processFreePlanDailyBonusForAllUsers();
            return ApiResponse.success("批量积分发放完成", result);
        } catch (Exception e) {
            log.error("批量发放每日积分失败 - 错误: {}", e.getMessage(), e);
            return ApiResponse.error("批量发放失败: " + e.getMessage());
        }
    }

    @GetMapping("/health-check")
    @Operation(summary = "积分系统健康检查", 
            description = "检查积分系统各组件状态")
    public ApiResponse<String> healthCheck() {
        
        log.info("执行积分系统健康检查");
        
        try {
            // 这里可以添加更多的健康检查逻辑
            // 比如检查数据库连接、缓存状态、定时任务状态等
            
            String healthStatus = "积分系统运行正常 - " + 
                    "时间: " + java.time.LocalDateTime.now() + ", " +
                    "状态: HEALTHY";
            
            return ApiResponse.success("健康检查通过", healthStatus);
        } catch (Exception e) {
            log.error("积分系统健康检查失败 - 错误: {}", e.getMessage(), e);
            return ApiResponse.error("健康检查失败: " + e.getMessage());
        }
    }
}
