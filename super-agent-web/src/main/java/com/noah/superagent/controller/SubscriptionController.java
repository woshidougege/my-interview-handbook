package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.PlansListResponse;
import com.noah.superagent.convert.SubscriptionPlanWebConvert;
import com.noah.superagent.model.UserSubscriptionDTO;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.response.UserSubscriptionWithCreditResponse;
import com.noah.superagent.service.SubscriptionPlanService;
import com.noah.superagent.service.UserSubscriptionService;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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

    /**
     * 按年订阅优惠比例（Web层配置）
     */
    @Value("${super-agent.billing.subscription.yearly-discount-rate:0.17}")
    private Double yearlyDiscountRate;

    @Operation(
        summary = "获取所有启用的套餐", 
        description = "获取系统中所有启用状态的订阅套餐列表，包含套餐详情、完整价格信息、功能特性等。包含月价、年价的原价和优惠信息，以及用户当前套餐标识。此接口无需认证，可用于展示给未登录用户。",
        tags = {"订阅管理"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "成功响应示例（真实数据）",
                        description = "返回所有启用的套餐列表，包含免费版、基础版、高级版等",
                        value = "{" +
                        "\"code\": 200," +
                        "\"message\": \"获取套餐列表成功\"," +
                        "\"data\": [" +
                            "{" +
                                "\"id\": 1," +
                                "\"planName\": \"免费版\"," +
                                "\"description\": \"适合轻度使用的个人用户\"," +
                                "\"features\": [" +
                                    "{\"text\": \"新用户赠送1000积分(90天有效)\", \"highlight\": true, \"included\": true}," +
                                    "{\"text\": \"每日登录赠300积分\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"分享新用户奖励500积分\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"公共数字分身(限制体验)\", \"highlight\": true, \"included\": true}" +
                                "]," +
                                "\"price\": 0," +
                                "\"monthlyPrice\": 0," +
                                "\"yearlyPrice\": 0," +
                                "\"validityDays\": 90," +
                                "\"enabled\": true," +
                                "\"isRecommended\": false," +
                                "\"sortOrder\": 1" +
                            "}," +
                            "{" +
                                "\"id\": 2," +
                                "\"planName\": \"基础版\"," +
                                "\"description\": \"适合中度使用的专业用户\"," +
                                "\"features\": [" +
                                    "{\"text\": \"一次性获得1900永久积分\", \"highlight\": true, \"included\": true}," +
                                    "{\"text\": \"享受所有免费版权益\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"访问限定天\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"公共数字分身\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"幻灯片制作\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"网站开发\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"数据分析\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"图片、视频生成\", \"highlight\": true, \"included\": true}" +
                                "]," +
                                "\"price\": 39," +
                                "\"monthlyPrice\": 39," +
                                "\"yearlyPrice\": 388," +
                                "\"validityDays\": 30," +
                                "\"enabled\": true," +
                                "\"isRecommended\": true," +
                                "\"sortOrder\": 2" +
                            "}," +
                            "{" +
                                "\"id\": 3," +
                                "\"planName\": \"高级版\"," +
                                "\"description\": \"适合重度使用的企业用户\"," +
                                "\"features\": [" +
                                    "{\"text\": \"一次性获得19000永久积分\", \"highlight\": true, \"included\": true}," +
                                    "{\"text\": \"享受所有免费版权益\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"专属数字分身\", \"highlight\": true, \"included\": true}," +
                                    "{\"text\": \"本机电脑操控\", \"highlight\": true, \"included\": true}" +
                                "]," +
                                "\"price\": 199," +
                                "\"monthlyPrice\": 199," +
                                "\"yearlyPrice\": 1983," +
                                "\"validityDays\": 30," +
                                "\"enabled\": true," +
                                "\"isRecommended\": false," +
                                "\"sortOrder\": 3" +
                            "}" +
                        "]," +
                        "\"success\": true," +
                        "\"timestamp\": 1736752800000" +
                    "}"
                    ),
                    @ExampleObject(
                        name = "空套餐列表",
                        description = "当系统中没有启用的套餐时的响应",
                        value = "{" +
                            "\"code\": 200," +
                            "\"message\": \"获取套餐列表成功\"," +
                            "\"data\": []," +
                            "\"success\": true," +
                            "\"timestamp\": 1736752800000" +
                        "}"
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "系统内部错误",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "系统错误示例",
                        description = "服务器内部错误时的响应格式",
                        value = "{" +
                            "\"code\": 500," +
                            "\"message\": \"系统内部错误\"," +
                            "\"data\": null," +
                            "\"success\": false," +
                            "\"timestamp\": 1736752800000" +
                        "}"
                    )
                }
            )
        )
    })
    @GetMapping("/plans")
    public ApiResponse<PlansListResponse> getPlans() {
        
        // 获取当前用户ID（如果已登录）
        Long currentUserId;
        Long currentUserPlanId = null;
        try {
            currentUserId = UserContext.getCurrentUserId(); // 使用可空版本，未登录不抛异常
            if (currentUserId != null) {
                // 获取用户当前有效订阅
                UserSubscriptionDTO currentSubscription = userSubscriptionService.getCurrentActiveSubscription(currentUserId);
                if (currentSubscription != null) {
                    currentUserPlanId = currentSubscription.getPlanId();
                }
            }
        } catch (Exception e) {
            // 忽略获取用户信息失败的情况，继续返回套餐列表
            log.debug("获取当前用户信息失败，继续返回套餐列表 - 错误: {}", e.getMessage());
        }
        
        // Service层返回DTO（配置文件中已包含完整价格信息）
        List<com.noah.superagent.model.SubscriptionPlanDTO> planDTOs = subscriptionPlanService.getEnabledPlans();
        
        // 标识当前套餐（价格信息已在Service层计算完成）
        final Long finalCurrentUserPlanId = currentUserPlanId;
        planDTOs.forEach(planDTO -> {
            planDTO.setIsCurrentPlan(finalCurrentUserPlanId != null && finalCurrentUserPlanId.equals(planDTO.getId()));
        });
        
        // Web层组装Response
        PlansListResponse response = new PlansListResponse();
        response.setYearlyDiscountRate(yearlyDiscountRate);
        response.setDiscountPercentageText(Math.round(yearlyDiscountRate * 100) + "%");
        response.setPlans(webConvert.toResponseList(planDTOs));
        
        return ApiResponse.success("获取套餐列表成功", response);
    }

    @Operation(
        summary = "获取当前登录用户的有效订阅信息以及可用积分总数",
        description = "获取当前登录用户的有效订阅信息以及可用积分总数。该接口整合了订阅和积分信息，一次调用即可获取用户的完整状态。" +
            "功能说明：" +
            "- 查询用户当前有效的订阅信息（如果存在）" +
            "- 查询用户可用积分总数" +
            "- 查询用户是否有积分账户" +
            "- 积分查询失败不影响订阅信息返回。" +
            "返回数据说明：" +
            "- subscription: 用户当前有效订阅，无订阅时为null" +
            "- availableCredits: 用户可用积分总数" +
            "- hasCreditAccount: 用户是否有积分账户",
        tags = {"订阅管理"}
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "有订阅有积分",
                        description = "用户有活跃订阅且有积分余额",
                        value = "{" +
                            "\"code\": 200," +
                            "\"message\": \"获取订阅和积分信息成功\"," +
                            "\"data\": {" +
                                "\"subscription\": {" +
                                    "\"id\": 12345," +
                                    "\"userId\": 1001," +
                                    "\"planId\": 2," +
                                    "\"startTime\": \"2025-01-01T10:00:00\"," +
                                    "\"endTime\": \"2025-02-01T10:00:00\"," +
                                    "\"paidAmount\": 39.9," +
                                    "\"status\": \"ACTIVE\"," +
                                    "\"payOrderNo\": \"ORDER_2025010110001\"," +
                                    "\"remark\": \"基础版套餐订阅\"" +
                                "}," +
                                "\"availableCredits\": 3500," +
                                "\"hasCreditAccount\": true" +
                            "}," +
                            "\"success\": true," +
                            "\"timestamp\": 1736752800000" +
                        "}"
                    ),
                    @ExampleObject(
                        name = "无订阅有积分",
                        description = "用户无活跃订阅但有积分余额",
                        value = "{" +
                            "\"code\": 200," +
                            "\"message\": \"获取订阅和积分信息成功\"," +
                            "\"data\": {" +
                                "\"subscription\": null," +
                                "\"availableCredits\": 1000," +
                                "\"hasCreditAccount\": true" +
                            "}," +
                            "\"success\": true," +
                            "\"timestamp\": 1736752800000" +
                        "}"
                    ),
                    @ExampleObject(
                        name = "无订阅无积分",
                        description = "用户无订阅且无积分账户",
                        value = "{" +
                            "\"code\": 200," +
                            "\"message\": \"获取订阅和积分信息成功\"," +
                            "\"data\": {" +
                                "\"subscription\": null," +
                                "\"availableCredits\": 0," +
                                "\"hasCreditAccount\": false" +
                            "}," +
                            "\"success\": true," +
                            "\"timestamp\": 1736752800000" +
                        "}"
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "未登录或token无效",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "未授权示例",
                    value = "{" +
                        "\"code\": 401," +
                        "\"message\": \"未授权\"," +
                        "\"data\": null," +
                        "\"success\": false," +
                        "\"timestamp\": 1736752800000" +
                    "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "权限不足",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "权限不足示例",
                    value = "{" +
                        "\"code\": 403," +
                        "\"message\": \"禁止访问\"," +
                        "\"data\": null," +
                        "\"success\": false," +
                        "\"timestamp\": 1736752800000" +
                    "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "系统内部错误",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "系统错误示例",
                    value = "{" +
                        "\"code\": 500," +
                        "\"message\": \"查询失败: 数据库连接异常\"," +
                        "\"data\": null," +
                        "\"success\": false," +
                        "\"timestamp\": 1736752800000" +
                    "}"
                )
            )
        )
    })
    @GetMapping("/current")
    public ApiResponse<UserSubscriptionWithCreditResponse> getCurrentSubscriptionWithCredit() {
        
        Long userId = UserContext.requireCurrentUserId();
        log.info("查询用户当前订阅和积分信息 - userId: {}", userId);
        
        try {
            // 查询用户当前有效订阅
            UserSubscriptionDTO subscription = userSubscriptionService.getCurrentActiveSubscription(userId);
            
            // 查询用户可用积分
            long availableCredits = 0L;
            boolean hasCreditAccount = false;
            String planName = null;
            String planCode = null;
            long limitedCredits = 0L;
            long dailyRefreshCredits = 0L;
            long permanentCredits = 0L;
            
            // 获取套餐名称和代码
            if (subscription != null && subscription.getPlanId() != null) {
                try {
                    var plan = subscriptionPlanService.getPlanById(subscription.getPlanId());
                    if (plan != null) {
                        planName = plan.getPlanName();
                        planCode = plan.getPlanCode() != null ? plan.getPlanCode().getCode() : null;
                    }
                } catch (Exception e) {
                    log.warn("查询套餐信息失败 - planId: {}, 错误: {}", subscription.getPlanId(), e.getMessage());
                }
            }
            
            try {
                hasCreditAccount = userCreditService.hasUserCredit(userId);
                if (!hasCreditAccount) {
                    // 用户首次登录，自动初始化积分账户
                    log.info("用户首次登录，自动初始化积分账户 - userId: {}", userId);
                    userCreditService.initFreePlanForUser(userId);
                    
                    // TODO: 创建免费套餐订阅记录
                    // 暂时跳过订阅记录创建，专注解决积分详情问题
                    
                    // 立即发放当日积分（使用新的登录时发放逻辑）
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
                        availableCredits = creditDetail.getTotalBalance() != null ? creditDetail.getTotalBalance().longValue() : 0L;

                        // 限时积分 = 免费积分 + 活动积分
                        limitedCredits = (creditDetail.getFreeBalance() != null ? creditDetail.getFreeBalance().longValue() : 0L) +
                                        (creditDetail.getActivityBalance() != null ? creditDetail.getActivityBalance().longValue() : 0L);

                        // 当日刷新积分
                        dailyRefreshCredits = creditDetail.getDailyBalance() != null ? creditDetail.getDailyBalance().longValue() : 0L;

                        // 永久积分
                        permanentCredits = creditDetail.getPermanentBalance() != null ? creditDetail.getPermanentBalance().longValue() : 0L;

                        log.info("积分详情 - userId: {}, total: {}, free: {}, activity: {}, daily: {}, permanent: {}",
                                userId, availableCredits,
                                creditDetail.getFreeBalance(),
                                creditDetail.getActivityBalance(),
                                creditDetail.getDailyBalance(),
                                creditDetail.getPermanentBalance());
                    }
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
            response.setPlanName(planName);
            response.setPlanCode(planCode);
            response.setLimitedCredits(limitedCredits);
            response.setDailyRefreshCredits(dailyRefreshCredits);
            response.setPermanentCredits(permanentCredits);
            
            // 明确指定泛型类型 - 解决Swagger嵌套对象显示问题
            return ApiResponse.success("获取订阅和积分信息成功", response);
            
        } catch (Exception e) {
            log.error("查询用户订阅和积分信息失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}
