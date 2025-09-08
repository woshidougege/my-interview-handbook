package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.CreditTransactionResponse;
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
 * 用户积分控制器
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user-credit")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户积分管理", description = "用户积分账户查询和积分交易记录查询接口")
public class UserCreditController {

    private final UserCreditService userCreditService;

    @GetMapping("/{userId}")
    @Operation(summary = "查询用户积分账户", 
            description = "根据用户ID查询用户的积分账户信息，包括总积分、免费积分、包月积分、永久积分等")
    public ApiResponse<UserCreditResponse> getUserCredit(
            @Parameter(description = "用户ID", required = true, example = "1234567890123456789")
            @PathVariable @NotNull @Min(1) Long userId) {
        
        log.info("查询用户积分账户 - userId: {}", userId);
        
        try {
            UserCreditResponse creditInfo = userCreditService.getUserCredit(userId);
            return ApiResponse.success("查询成功", creditInfo);
        } catch (Exception e) {
            log.error("查询用户积分账户失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/transactions")
    @Operation(summary = "查询用户积分交易记录", 
            description = "分页查询用户的积分交易记录，包括充值、消费、过期等各种交易类型")
    public ApiResponse<PageResponse<CreditTransactionResponse>> getCreditTransactions(
            @Parameter(description = "用户ID", required = true, example = "1234567890123456789")
            @PathVariable @NotNull @Min(1) Long userId,
            @Parameter(description = "页码", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") @Min(1) Integer pageNum,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) Integer pageSize) {
        
        log.info("查询用户积分交易记录 - userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        
        try {
            PageResponse<CreditTransactionResponse> transactions = 
                    userCreditService.getCreditTransactions(userId, pageNum, pageSize);
            return ApiResponse.success("查询成功", transactions);
        } catch (Exception e) {
            log.error("查询用户积分交易记录失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }


    @GetMapping("/{userId}/available")
    @Operation(summary = "查询用户可用积分", 
            description = "快速查询用户当前可用的积分总额")
    public ApiResponse<Long> getAvailableCredits(
            @Parameter(description = "用户ID", required = true, example = "1234567890123456789")
            @PathVariable @NotNull @Min(1) Long userId) {
        
        log.info("查询用户可用积分 - userId: {}", userId);
        
        try {
            Long availableCredits = userCreditService.getAvailableCredits(userId);
            return ApiResponse.success("查询成功", availableCredits);
        } catch (Exception e) {
            log.error("查询用户可用积分失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/exists")
    @Operation(summary = "检查用户积分账户是否存在", 
            description = "检查指定用户是否已创建积分账户")
    public ApiResponse<Boolean> hasUserCredit(
            @Parameter(description = "用户ID", required = true, example = "1234567890123456789")
            @PathVariable @NotNull @Min(1) Long userId) {
        
        log.info("检查用户积分账户是否存在 - userId: {}", userId);
        
        try {
            boolean exists = userCreditService.hasUserCredit(userId);
            return ApiResponse.success("查询成功", exists);
        } catch (Exception e) {
            log.error("检查用户积分账户失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}
