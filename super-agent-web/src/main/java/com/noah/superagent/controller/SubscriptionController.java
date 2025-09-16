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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Operation(
        summary = "获取所有启用的套餐", 
        description = "获取系统中所有启用状态的订阅套餐列表，包含套餐详情、价格、功能特性等信息。此接口无需认证，可用于展示给未登录用户。",
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
                                "\"creditAmount\": 1000," +
                                "\"monthlyCreditAmount\": 0," +
                                "\"yearlyCreditAmount\": 0," +
                                "\"dailyRefreshCredit\": 0," +
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
                                "\"creditAmount\": 1900," +
                                "\"monthlyCreditAmount\": 1900," +
                                "\"yearlyCreditAmount\": 1900," +
                                "\"dailyRefreshCredit\": 0," +
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
                                "\"creditAmount\": 19000," +
                                "\"monthlyCreditAmount\": 19000," +
                                "\"yearlyCreditAmount\": 19000," +
                                "\"dailyRefreshCredit\": 0," +
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
    public ApiResponse<List<SubscriptionPlanResponse>> getPlans() {
        List<SubscriptionPlanResponse> plans = webConvert.toResponseList(
            subscriptionPlanService.getEnabledPlans()
        );
        
        // 明确指定泛型类型 - 解决Swagger嵌套对象显示问题
        return ApiResponse.success("获取套餐列表成功", plans);
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
                                    "\"creditAmount\": 5000," +
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
            
            // 明确指定泛型类型 - 解决Swagger嵌套对象显示问题
            return ApiResponse.success("获取订阅和积分信息成功", response);
            
        } catch (Exception e) {
            log.error("查询用户订阅和积分信息失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}
