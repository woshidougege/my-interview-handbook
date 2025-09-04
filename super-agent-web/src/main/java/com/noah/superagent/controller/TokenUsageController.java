package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.TokenUsageRequest;
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
    @Operation(summary = "上报Token使用量", description = "智能体上报Token使用量，异步处理，快速响应，支持幂等性")
    public ApiResponse<String> reportTokenUsage(@Valid @RequestBody TokenUsageRequest request) {
        log.info("接收Token使用量上报 - requestId: {}, userKey: {}, agentId: {}, inputTokens: {}, outputTokens: {}", 
            request.getRequestId(), request.getUserId(), request.getAgentId(),
            request.getInputTokens(), request.getOutputTokens());

        try {
            // 异步处理，立即返回
            tokenUsageService.recordTokenUsageAsync(request);
            return ApiResponse.success("上报成功", request.getRequestId());
            
        } catch (Exception e) {
            log.error("Token使用量上报失败 - requestId: {}, 错误: {}", 
                request.getRequestId(), e.getMessage(), e);
            return ApiResponse.error("系统异常: " + e.getMessage());
        }
    }
}
