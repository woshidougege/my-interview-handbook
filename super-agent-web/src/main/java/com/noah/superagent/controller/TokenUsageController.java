package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.TokenUsageRequest;
import com.noah.superagent.common.dto.response.TokenUsageResponse;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.token.service.TokenUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Token使用量记录控制器 - 简化版
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/token-usage")
@RequiredArgsConstructor
@Validated
@Tag(name = "Token使用量记录", description = "智能体Token使用量上报接口")
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    @PostMapping("/report")
    @Operation(summary = "上报Token使用量", description = "智能体上报Token使用量，支持幂等性")
    public ApiResponse<TokenUsageResponse> reportTokenUsage(@Valid @RequestBody TokenUsageRequest request) {
        long startTime = System.currentTimeMillis();
        
        log.info("接收Token使用量上报 - requestId: {}, userKey: {}, agentId: {}, inputTokens: {}, outputTokens: {}", 
            request.getRequestId(), request.getUserExternalKey(), request.getAgentId(), 
            request.getInputTokens(), request.getOutputTokens());

        try {
            TokenUsageResponse response = tokenUsageService.recordTokenUsage(request);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Token使用量上报完成 - requestId: {}, success: {}, 耗时: {}ms", 
                request.getRequestId(), response.isSuccess(), processingTime);

            if (response.isSuccess()) {
                return ApiResponse.success("上报成功", response);
            } else {
                return ApiResponse.badRequest(response.getMessage());
            }
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Token使用量上报失败 - requestId: {}, 耗时: {}ms, 错误: {}", 
                request.getRequestId(), processingTime, e.getMessage(), e);
            
            return ApiResponse.error("系统异常: " + e.getMessage());
        }
    }
}
