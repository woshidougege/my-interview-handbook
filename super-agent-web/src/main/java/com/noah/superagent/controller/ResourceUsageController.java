package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.ResourceUsageRequest;
import com.noah.superagent.common.enums.ResourceTypeEnum;
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
    @Operation(summary = "上报资源使用量", 
            description = "子智能体每次任务完成后上报资源使用量，支持Token、功能调用、媒体生成等多种资源类型，异步处理，快速响应，支持幂等性。\n\n" +
                    "**重要说明**\n" +
                    "- requestId是幂等性控制的关键，同一个任务重试时必须使用相同的requestId\n" +
                    "- 推荐使用格式：{前缀}_{用户ID}_{会话ID}_{任务序号}_{时间戳}\n" +
                    "- 示例：req_user123_context789_001_1704701234567\n\n" +
                    "**requestId生成规则**\n" +
                    "1. 必须保证同一个任务重试时requestId相同\n" +
                    "2. 不同任务必须有不同的requestId\n" +
                    "3. 建议包含用户ID、会话ID、任务序号等信息便于追踪\n" +
                    "4. 可以使用时间戳，但要确保同一任务的重试使用相同值\n\n" +
                    "**错误处理**\n" +
                    "- 重复requestId会被系统识别并返回成功响应，但不会重复计费\n" +
                    "- 系统会记录第一次上报的数据作为最终结果")
    public ApiResponse<String> reportResourceUsage(@Valid @RequestBody ResourceUsageRequest request) {
        log.info("接收资源使用量上报 - requestId: {}, userId: {}, agentId: {}, taskType: {}", 
            request.getRequestId(), request.getUserId(), request.getAgentId(), request.getTaskType() != null ? request.getTaskType().name() : null);

        // 详细日志记录
        ResourceUsageRequest.ResourceUsageDetail usageDetail = request.getUsageDetail();
        if (usageDetail != null) {
            log.info("资源使用详情 - 资源类型: {}, 描述: {}", 
                usageDetail.getResourceType() != null ? usageDetail.getResourceType().name() : null, 
                usageDetail.getDescription());
            
            ResourceTypeEnum resourceType = usageDetail.getResourceType();
            if (resourceType != null) {
                switch (resourceType) {
                    case TOKEN:
                        log.debug("Token使用 - 输入Token: {}, 输出Token: {}", 
                            usageDetail.getInputTokens(), usageDetail.getOutputTokens());
                        break;
                    case IMAGE_COUNT:
                        log.debug("图片生成 - 数量: {}", usageDetail.getImageCount());
                        break;
                    case VIDEO_DURATION:
                        log.debug("视频生成 - 时长: {}秒", usageDetail.getVideoDuration());
                        break;
                    case PPT_PAGES:
                        log.debug("PPT生成 - 页数: {}", usageDetail.getPptPages());
                        break;
                    case FUNCTION_TIMES:
                        log.debug("功能使用 - 次数: {}", usageDetail.getFunctionTimes());
                        break;
                }
            }
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
