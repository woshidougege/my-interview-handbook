package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.ResourceUsageRequest;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.ResourceUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 资源使用量记录控制器
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resource-usage")
@RequiredArgsConstructor
@Validated
@Tag(name = "资源使用量记录", description = "智能体资源使用量上报接口，支持Token、功能、媒体等多种资源类型")
public class ResourceUsageController {

    private final ResourceUsageService resourceUsageService;

    @PostMapping("/report")
    @Operation(summary = "上报资源使用量", description = "智能体上报资源使用量，支持Token、功能调用、媒体生成等多种资源类型，异步处理，快速响应，支持幂等性")
    public ApiResponse<String> reportResourceUsage(@Valid @RequestBody ResourceUsageRequest request) {
        log.info("接收资源使用量上报 - requestId: {}, userId: {}, agentId: {}, taskType: {}", 
            request.getRequestId(), request.getUserId(), request.getAgentId(), request.getTaskType());

        // 详细日志记录
        if (request.getModelUsages() != null && !request.getModelUsages().isEmpty()) {
            log.info("模型使用 - 共{}项", request.getModelUsages().size());
            request.getModelUsages().forEach(usage -> 
                log.debug("模型: {}, 类型: {}, 输入Token: {}, 输出Token: {}", 
                    usage.getModelName(), usage.getModelType(), usage.getInputTokens(), usage.getOutputTokens()));
        }
        
        if (request.getFunctionUsages() != null && !request.getFunctionUsages().isEmpty()) {
            log.info("功能使用 - 共{}项", request.getFunctionUsages().size());
            request.getFunctionUsages().forEach(usage -> 
                log.debug("功能: {}, 使用次数: {}, 计费单位: {}", 
                    usage.getFunctionType(), usage.getUsageCount(), usage.getBillingUnit()));
        }
        
        if (request.getMediaUsages() != null && !request.getMediaUsages().isEmpty()) {
            log.info("媒体使用 - 共{}项", request.getMediaUsages().size());
            request.getMediaUsages().forEach(usage -> 
                log.debug("媒体: {}, 使用量: {}, 计费单位: {}", 
                    usage.getMediaType(), usage.getUsageAmount(), usage.getBillingUnit()));
        }

        try {
            // 异步处理，立即返回
            resourceUsageService.recordResourceUsageAsync(request);
            return ApiResponse.success("上报成功", request.getRequestId());
            
        } catch (Exception e) {
            log.error("资源使用量上报失败 - requestId: {}, 错误: {}", 
                request.getRequestId(), e.getMessage(), e);
            return ApiResponse.error("系统异常: " + e.getMessage());
        }
    }
}
