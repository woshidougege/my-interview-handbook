package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.PlansListResponse;
import com.noah.superagent.common.dto.response.CreditPurchaseConfigResponse;
import com.noah.superagent.common.config.CreditPurchaseConfig;
import com.noah.superagent.convert.SubscriptionPlanWebConvert;
import com.noah.superagent.model.UserSubscriptionDTO;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.response.UserSubscriptionResponse;
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
    private final CreditPurchaseConfig creditPurchaseConfig;

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
                        "\"data\": {" +
                            "\"yearlyDiscountRate\": 0.17," +
                            "\"discountPercentageText\": \"17%\"," +
                            "\"plans\": [" +
                            "{" +
                                "\"id\": 1," +
                                "\"planName\": \"免费版\"," +
                                "\"description\": \"适合轻度使用的个人用户\"," +
                                "\"planCode\": \"FREE\"," +
                                "\"features\": [" +
                                    "{\"text\": \"每日可获得***新积分\", \"highlight\": true, \"included\": true}," +
                                    "{\"text\": \"访问聊天\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"公共数字分身（限时体验）\", \"highlight\": false, \"included\": true}" +
                                "]," +
                                "\"monthlyOriginalPrice\": 0.00," +
                                "\"monthlyPrice\": 0.00," +
                                "\"yearlyOriginalPrice\": 0.00," +
                                "\"yearlyPrice\": 0.00," +
                                "\"monthlySavings\": 0.00," +
                                "\"yearlySavings\": 0.00," +
                                "\"validityDays\": 90," +
                                "\"enabled\": true," +
                                "\"isRecommended\": false," +
                                "\"isCurrentPlan\": false," +
                                "\"sortOrder\": 1" +
                            "}," +
                            "{" +
                                "\"id\": 2," +
                                "\"planName\": \"基础版\"," +
                                "\"planCode\": \"BASIC\"," +
                                "\"description\": \"适合中度使用的专业用户\"," +
                                "\"features\": [" +
                                    "{\"text\": \"一次性发放1900积分\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"每日可获得300新积分\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"访问聊天\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"公共数字分身\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"幻灯片制作\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"网站开发\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"数据分析\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"图片、视频生成\", \"highlight\": false, \"included\": true}" +
                                "]," +
                                "\"monthlyOriginalPrice\": 39.00," +
                                "\"monthlyPrice\": 29.00," +
                                "\"yearlyOriginalPrice\": 468.00," +
                                "\"yearlyPrice\": 388.44," +
                                "\"monthlySavings\": 10.00," +
                                "\"yearlySavings\": 79.56," +
                                "\"validityDays\": 30," +
                                "\"enabled\": true," +
                                "\"isRecommended\": true," +
                                "\"isCurrentPlan\": false," +
                                "\"sortOrder\": 2" +
                            "}," +
                            "{" +
                                "\"id\": 3," +
                                "\"planName\": \"高级版\"," +
                                "\"planCode\": \"PREMIUM\"," +
                                "\"description\": \"适合重度使用的企业用户\"," +
                                "\"features\": [" +
                                    "{\"text\": \"一次性发放1900积分\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"每日可获得300新积分\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"访问聊天\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"专属数字分身\", \"highlight\": true, \"included\": true}," +
                                    "{\"text\": \"幻灯片制作\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"网站开发\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"数据分析\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"图片、视频生成\", \"highlight\": false, \"included\": true}," +
                                    "{\"text\": \"本机电脑操控\", \"highlight\": true, \"included\": true}" +
                                "]," +
                                "\"monthlyOriginalPrice\": 199.00," +
                                "\"monthlyPrice\": 179.00," +
                                "\"yearlyOriginalPrice\": 2388.00," +
                                "\"yearlyPrice\": 1982.04," +
                                "\"monthlySavings\": 20.00," +
                                "\"yearlySavings\": 405.96," +
                                "\"validityDays\": 30," +
                                "\"enabled\": true," +
                                "\"isRecommended\": false," +
                                "\"isCurrentPlan\": false," +
                                "\"sortOrder\": 3" +
                            "}" +
                            "]" +
                        "}," +
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
                            "\"data\": {" +
                                "\"yearlyDiscountRate\": 0.17," +
                                "\"discountPercentageText\": \"17%\"," +
                                "\"plans\": []" +
                            "}," +
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
        summary = "获取当前登录用户的有效订阅信息",
        description = "获取当前登录用户的有效订阅信息。该接口仅返回订阅相关信息，积分信息通过独立接口获取。" +
            "功能说明：" +
            "- 查询用户当前有效的订阅信息（如果存在）" +
            "- 查询套餐名称和代码" +
            "返回数据说明：" +
            "- subscription: 用户当前有效订阅，无订阅时为null" +
            "- planName: 套餐名称" +
            "- planCode: 套餐代码",
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
    public ApiResponse<UserSubscriptionResponse> getCurrentSubscription() {
        
        Long userId = UserContext.requireCurrentUserId();
        log.info("查询用户当前订阅信息 - userId: {}", userId);
        
        try {
            // 查询用户当前有效订阅
            UserSubscriptionDTO subscription = userSubscriptionService.getCurrentActiveSubscription(userId);
            
            String planName = null;
            String planCode = null;
            
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
            
            // 构建响应
            UserSubscriptionResponse response = new UserSubscriptionResponse();
            response.setSubscription(subscription);
            response.setPlanName(planName);
            response.setPlanCode(planCode);
            
            return ApiResponse.success("获取订阅信息成功", response);
            
        } catch (Exception e) {
            log.error("获取用户订阅信息失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(
        summary = "获取积分购买配置",
        description = "获取购买积分窗口所需的数据，包括价格配置、当前套餐信息和积分有效期",
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
                        name = "成功响应示例",
                        description = "返回积分购买配置信息",
                        value = "{" +
                        "\"code\": 200," +
                        "\"message\": \"获取积分购买配置成功\"," +
                        "\"data\": {" +
                            "\"planName\": \"基础版\"," +
                            "\"planCode\": \"basic\"," +
                            "\"creditValidityDays\": 0," +
                            "\"availableCredits\": \"3200\"," +
                            "\"dailyRefreshCredits\": \"300\"," +
                            "\"limitedCredits\": \"0\"," +
                            "\"creditPackages\": [" +
                            "{" +
                                "\"id\": \"100\"," +
                                "\"name\": \"10000积分\"," +
                                "\"code\": \"CREDIT_PACK_10000\"," +
                                "\"creditsAmount\": 10000," +
                                "\"price\": 59," +
                                "\"isRecommended\": false," +
                                "\"features\": [\"约生成10-14个PPT\", \"约生成7-9个深度研究报告\"]" +
                            "}," +
                            "{" +
                                "\"id\": \"101\"," +
                                "\"name\": \"20000积分\"," +
                                "\"code\": \"CREDIT_PACK_20000\"," +
                                "\"creditsAmount\": 20000," +
                                "\"price\": 99," +
                                "\"isRecommended\": false," +
                                "\"features\": [\"约生成10-14个PPT\", \"约生成7-9个深度研究报告\"]" +
                            "}," +
                            "{" +
                                "\"id\": \"102\"," +
                                "\"name\": \"50000积分\"," +
                                "\"code\": \"CREDIT_PACK_50000\"," +
                                "\"creditsAmount\": 50000," +
                                "\"price\": 199," +
                                "\"isRecommended\": true," +
                                "\"features\": [\"约生成10-14个PPT\", \"约生成7-9个深度研究报告\"]" +
                            "}" +
                            "]" +
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
            description = "未登录或token无效"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "系统内部错误"
        )
    })
    @GetMapping("/credit-purchase-config")
    public ApiResponse<CreditPurchaseConfigResponse> getCreditPurchaseConfig() {
        
        Long userId = UserContext.requireCurrentUserId();
        log.info("获取用户积分购买配置 - userId: {}", userId);
        
        try {
            CreditPurchaseConfigResponse response = new CreditPurchaseConfigResponse();
            
            // 获取当前用户订阅信息
            String planName = "免费版";
            String planCode = "free";
            Integer planDailyRefreshCredits = 300; // 默认免费版每日刷新积分
            
            try {
                UserSubscriptionDTO subscription = userSubscriptionService.getCurrentActiveSubscription(userId);
                if (subscription != null && subscription.getPlanId() != null) {
                    var plan = subscriptionPlanService.getPlanById(subscription.getPlanId());
                    if (plan != null) {
                        planName = plan.getPlanName();
                        planCode = plan.getPlanCode() != null ? plan.getPlanCode().getCode() : null;
                        planDailyRefreshCredits = plan.getDailyRefreshCredits();
                    }
                }
            } catch (Exception e) {
                log.warn("查询用户订阅信息失败 - userId: {}, 错误: {}", userId, e.getMessage());
            }
            
            // 获取当前用户积分信息
            String availableCredits = "0";
            String dailyRefreshCredits = "0";
            String limitedCredits = "0";
            
            try {
                boolean hasCreditAccount = userCreditService.hasUserCredit(userId);
                if (!hasCreditAccount) {
                    // 用户首次访问，自动初始化积分账户
                    userCreditService.initFreePlanForUser(userId);
                    hasCreditAccount = true;
                }

                if (hasCreditAccount) {
                    var creditDetail = userCreditService.getUserCredit(userId);
                    if (creditDetail != null) {
                        long totalBalance = creditDetail.getTotalBalance() != null ? creditDetail.getTotalBalance().longValue() : 0L;
                        long limitedCreditsLong = (creditDetail.getFreeBalance() != null ? creditDetail.getFreeBalance().longValue() : 0L) +
                                                (creditDetail.getActivityBalance() != null ? creditDetail.getActivityBalance().longValue() : 0L);
                        long dailyRefreshCreditsLong = creditDetail.getDailyBalance() != null ? creditDetail.getDailyBalance().longValue() : 0L;
                        
                        availableCredits = String.valueOf(totalBalance);
                        dailyRefreshCredits = String.valueOf(dailyRefreshCreditsLong);
                        limitedCredits = String.valueOf(limitedCreditsLong);
                    }
                }
            } catch (Exception e) {
                log.warn("查询用户积分信息失败 - userId: {}, 错误: {}", userId, e.getMessage());
            }
            
            response.setPlanName(planName);
            response.setPlanCode(planCode);
            response.setAvailableCredits(availableCredits);
            response.setDailyRefreshCredits(dailyRefreshCredits);
            response.setLimitedCredits(limitedCredits);
            response.setPlanDailyRefreshCredits(planDailyRefreshCredits);
            
            // 从积分购买配置中获取积分包信息
            List<CreditPurchaseConfigResponse.CreditPackageConfig> creditPackages = 
                creditPurchaseConfig.getPackages().stream()
                    .map(pkg -> {
                        CreditPurchaseConfigResponse.CreditPackageConfig config = new CreditPurchaseConfigResponse.CreditPackageConfig();
                        config.setId(pkg.getId());
                        config.setName(pkg.getName());
                        config.setCode("CREDIT_PACK_" + pkg.getId()); // 生成代码
                        config.setCreditsAmount(pkg.getCreditsAmount());
                        config.setPrice(pkg.getPrice());
                        config.setIsRecommended(pkg.getIsRecommended());
                        // 转换功能特性列表
                        List<CreditPurchaseConfigResponse.FeatureConfig> features = pkg.getFeatures().stream()
                            .map(feature -> {
                                CreditPurchaseConfigResponse.FeatureConfig featureConfig = new CreditPurchaseConfigResponse.FeatureConfig();
                                featureConfig.setText(feature.getText());
                                featureConfig.setHighlight(feature.getHighlight());
                                return featureConfig;
                            })
                            .collect(java.util.stream.Collectors.toList());
                        config.setFeatures(features);
                        
                        return config;
                    })
                    .sorted((a, b) -> a.getPrice().compareTo(b.getPrice())) // 按价格排序
                    .collect(java.util.stream.Collectors.toList());
            
            response.setCreditPackages(creditPackages);
            
            // 设置积分有效期（从配置中读取）
            response.setCreditValidityDays(creditPurchaseConfig.getValidityDays());
            
            return ApiResponse.success("获取积分购买配置成功", response);
            
        } catch (Exception e) {
            log.error("获取积分购买配置失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("获取配置失败: " + e.getMessage());
        }
    }
}
