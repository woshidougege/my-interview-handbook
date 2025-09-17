package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.UserCreditStatusResponse;
import com.noah.superagent.common.dto.response.CreditTransactionResponse;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户积分控制器
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/credit")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户积分", description = "用户积分相关接口")
public class UserCreditController {

    private final UserCreditService userCreditService;

    @Operation(
        summary = "获取当前用户积分完整状态",
        description = "获取当前登录用户的完整积分状态信息，包括:\n" +
                "- 总积分余额和各类型积分详情\n" +
                "- 透支额度、欠费状态、账户状态\n" +
                "- 积分过期情况和有效期信息\n" +
                "- 适用于积分管理界面的完整展示"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/status")
    public ApiResponse<UserCreditStatusResponse> getCreditStatus() {
        Long userId = UserContext.requireCurrentUserId();
        log.info("查询用户积分完整状态 - userId: {}", userId);
        
        try {
            UserCreditStatusResponse creditStatus = userCreditService.getUserCreditStatus(userId);
            return ApiResponse.success("获取积分状态成功", creditStatus);
        } catch (Exception e) {
            log.error("查询用户积分状态失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(
        summary = "获取当前用户积分详情",
        description = "获取当前登录用户的详细积分信息，包括各类型积分余额、累计获得/消费积分等"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/details")
    public ApiResponse<UserCreditResponse> getCreditDetails() {
        Long userId = UserContext.requireCurrentUserId();
        log.info("查询用户积分详情 - userId: {}", userId);
        
        try {
            UserCreditResponse creditDetails = userCreditService.getUserCredit(userId);
            return ApiResponse.success("获取积分详情成功", creditDetails);
        } catch (Exception e) {
            log.error("查询用户积分详情失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(
        summary = "分页查询积分交易记录",
        description = "分页查询当前登录用户的积分交易记录，包括获得积分、消费积分等操作记录"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<CreditTransactionResponse>> getCreditTransactions(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Long userId = UserContext.requireCurrentUserId();
        log.info("分页查询用户积分交易记录 - userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        
        try {
            PageResponse<CreditTransactionResponse> transactions = 
                userCreditService.getCreditTransactions(userId, pageNum, pageSize);
            return ApiResponse.success("查询积分交易记录成功", transactions);
        } catch (Exception e) {
            log.error("查询积分交易记录失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}
