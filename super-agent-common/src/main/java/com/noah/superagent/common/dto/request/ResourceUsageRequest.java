package com.noah.superagent.common.dto.request;

import com.noah.superagent.common.enums.ResourceTypeEnum;
import com.noah.superagent.common.enums.TaskTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 资源使用量上报请求
 * 子智能体每次任务完成后上报真实消耗数据
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "资源使用量上报请求")
public class ResourceUsageRequest {

    @NotBlank(message = "请求ID不能为空")
    @Schema(description = "请求ID（幂等键）\n" +
            "生成规则：必须保证同一个任务重试时requestId相同，不同任务requestId不同\n" +
            "推荐格式：{前缀}_{用户ID}_{会话ID}_{任务序号}_{时间戳}\n" +
            "示例：req_user123_context789_001_1704701234567\n" +
            "注意：这是幂等性控制的关键，重复上报相同requestId会被系统识别并忽略", 
            example = "req_user123_context789_001_1704701234567")
    private String requestId;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID（雪花算法生成的Long类型）", example = "1234567890123456789")
    private Long userId;

    @NotBlank(message = "智能体ID不能为空")
    @Schema(description = "智能体ID", example = "agent_12345")
    private String agentId;

    @Schema(description = "会话ID", example = "context_12345")
    private String contextId;

    @NotNull(message = "任务类型不能为空")
    @Schema(description = "任务类型", example = "TEXT_GENERATION")
    private TaskTypeEnum taskType;

    @Schema(description = "任务描述", example = "生成某行业调研报告任务")
    private String taskDescription;

    @Valid
    @Schema(description = "资源使用详情")
    private ResourceUsageDetail usageDetail;

    /**
     * 资源使用详情
     */
    @Data
    @Schema(description = "资源使用详情")
    public static class ResourceUsageDetail {
        
        @Schema(description = "资源类型", example = "TOKEN")
        private ResourceTypeEnum resourceType;
        
        @Schema(description = "模型名称（可选，用于区分不同模型的计费）", example = "qwen-plus")
        private String model;
        
        @Schema(description = "输入Token数量（仅文本生成时使用）", example = "3000")
        private Long inputTokens;
        
        @Schema(description = "输出Token数量（仅文本生成时使用）", example = "1000")
        private Long outputTokens;
        
        @Schema(description = "图片数量（仅图片生成时使用）", example = "2")
        private Integer imageCount;
        
        @Schema(description = "视频时长秒数（仅视频生成时使用）", example = "30")
        private Integer videoDuration;
        
        @Schema(description = "PPT页数（仅PPT生成时使用）", example = "10")
        private Integer pptPages;
        
        @Schema(description = "功能使用次数（所有功能类任务）", example = "1")
        private Integer functionTimes;
        
        @Schema(description = "资源使用描述", example = "生成行业调研报告内容")
        private String description;
    }
}
