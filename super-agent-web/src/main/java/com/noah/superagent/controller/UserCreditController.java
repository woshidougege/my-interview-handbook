package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.response.UserCreditResponse;
import com.noah.superagent.common.dto.response.UserCreditDetailsResponse;
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
        summary = "获取当前用户积分详情",
        description = "获取当前登录用户的积分详情，仅包含积分数据（总积分、当日刷新积分、永久积分、限时积分），订阅信息通过独立接口获取"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/details")
    public ApiResponse<UserCreditDetailsResponse> getCreditDetails() {
        Long userId = UserContext.requireCurrentUserId();
        log.info("查询用户积分详情 - userId: {}", userId);
        
        try {
            boolean hasCreditAccount = false;
            long totalBalance = 0L;
            long limitedCredits = 0L;
            long dailyRefreshCredits = 0L;
            long permanentCredits = 0L;
            
            // 检查用户是否有积分账户
            hasCreditAccount = userCreditService.hasUserCredit(userId);
            if (!hasCreditAccount) {
                // 用户首次访问，自动初始化积分账户
                log.info("用户首次访问，自动初始化积分账户 - userId: {}", userId);
                userCreditService.initFreePlanForUser(userId);
                
                // 立即发放当日积分
                try {
                    userCreditService.giveFreePlanDailyBonusOnLogin(userId);
                    log.info("用户首次登录积分发放成功 - userId: {}", userId);
                } catch (Exception dailyBonusError) {
                    log.warn("发放每日积分失败，但不影响账户初始化 - userId: {}, 错误: {}", userId, dailyBonusError.getMessage());
                }
                
                hasCreditAccount = true;
            }

            if (hasCreditAccount) {
                // 获取详细积分信息
                var creditDetail = userCreditService.getUserCredit(userId);
                if (creditDetail != null) {
                    totalBalance = creditDetail.getTotalBalance() != null ? creditDetail.getTotalBalance().longValue() : 0L;

                    // 限时积分 = 免费积分 + 活动积分
                    long freeCredits = creditDetail.getFreeBalance() != null ? creditDetail.getFreeBalance().longValue() : 0L;
                    long activityCredits = creditDetail.getActivityBalance() != null ? creditDetail.getActivityBalance().longValue() : 0L;
                    limitedCredits = freeCredits + activityCredits;

                    // 当日刷新积分
                    dailyRefreshCredits = creditDetail.getDailyBalance() != null ? creditDetail.getDailyBalance().longValue() : 0L;

                    // 永久积分
                    permanentCredits = creditDetail.getPermanentBalance() != null ? creditDetail.getPermanentBalance().longValue() : 0L;

                    // 添加详细日志来调试
                    log.info("积分详情调试 - userId: {}, total: {}, free: {}, activity: {}, daily: {}, permanent: {}, 计算后 - limited: {}", 
                            userId, totalBalance, freeCredits, activityCredits, dailyRefreshCredits, permanentCredits, limitedCredits);
                }
            }
            
            // 构建响应
            UserCreditDetailsResponse response = new UserCreditDetailsResponse();
            response.setTotalBalance(String.valueOf(totalBalance));
            response.setDailyRefreshCredits(String.valueOf(dailyRefreshCredits));
            response.setPermanentCredits(String.valueOf(permanentCredits));
            response.setLimitedCredits(String.valueOf(limitedCredits));
            response.setHasCreditAccount(hasCreditAccount);
            
            return ApiResponse.success("获取积分详情成功", response);
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
