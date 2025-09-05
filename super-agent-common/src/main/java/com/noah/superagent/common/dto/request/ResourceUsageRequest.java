package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 资源使用量上报请求
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "资源使用量上报请求")
public class ResourceUsageRequest {

    @NotBlank(message = "请求ID不能为空")
    @Schema(description = "请求ID（幂等键）", example = "req_20240101_agent123_session456_001")
    private String requestId;

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "user_67890")
    private String userId;

    @NotBlank(message = "智能体ID不能为空")
    @Schema(description = "智能体ID", example = "agent_12345")
    private String agentId;

    @Schema(description = "会话ID", example = "session_12345")
    private String sessionId;

    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型", example = "INDUSTRY_RESEARCH_REPORT", 
            allowableValues = {"INDUSTRY_RESEARCH_REPORT", "PPT_GENERATION", "CODE_GENERATION", "IMAGE_GENERATION", "VIDEO_GENERATION", "TRANSLATION", "DOCUMENT_WRITING"})
    private String taskType;

    @Schema(description = "任务描述", example = "生成某行业调研报告任务")
    private String taskDescription;

    @Valid
    @Schema(description = "模型使用记录列表")
    private List<ModelUsage> modelUsages;

    @Valid
    @Schema(description = "功能使用记录列表")
    private List<FunctionUsage> functionUsages;

    @Valid
    @Schema(description = "媒体生成记录列表")
    private List<MediaUsage> mediaUsages;

    /**
     * 模型使用记录
     */
    @Data
    @Schema(description = "模型使用记录")
    public static class ModelUsage {
        @NotBlank(message = "模型名称不能为空")
        @Schema(description = "模型名称", example = "gpt-4")
        private String modelName;

        @NotBlank(message = "模型类型不能为空")
        @Schema(description = "模型类型", example = "TEXT_GENERATION", 
                allowableValues = {"TEXT_GENERATION", "IMAGE_GENERATION", "VIDEO_GENERATION"})
        private String modelType;

        @Min(value = 0, message = "输入Token数不能为负数")
        @Schema(description = "输入Token数量", example = "3000")
        private Long inputTokens;

        @Min(value = 0, message = "输出Token数不能为负数")
        @Schema(description = "输出Token数量", example = "1000000")
        private Long outputTokens;

        @Schema(description = "使用描述", example = "深度搜索执行")
        private String description;
    }

    /**
     * 功能使用记录
     */
    @Data
    @Schema(description = "功能使用记录")
    public static class FunctionUsage {
        @NotBlank(message = "功能类型不能为空")
        @Schema(description = "功能类型", example = "DEEPSEARCH", 
                allowableValues = {"DEEPSEARCH", "BROWSERUSE", "SOFTWARE_OPERATION", "PPT_GENERATION", 
                                 "MEETING_MINUTES", "DOCUMENT_WRITING", "CODING", "TRANSLATION", 
                                 "MIND_MAP", "DATABASE_ANALYSIS", "EXCEL_ANALYSIS"})
        private String functionType;

        @NotNull(message = "使用次数不能为空")
        @Min(value = 1, message = "使用次数至少为1")
        @Schema(description = "使用次数", example = "1")
        private Integer usageCount;

        @Schema(description = "计费单位", example = "TIMES", 
                allowableValues = {"TIMES", "PAGES", "SECONDS"})
        private String billingUnit;

        @Schema(description = "功能描述", example = "执行深度搜索")
        private String description;
    }

    /**
     * 媒体使用记录
     */
    @Data
    @Schema(description = "媒体使用记录")
    public static class MediaUsage {
        @NotBlank(message = "媒体类型不能为空")
        @Schema(description = "媒体类型", example = "IMAGE", 
                allowableValues = {"IMAGE", "VIDEO"})
        private String mediaType;

        @NotNull(message = "使用量不能为空")
        @Min(value = 1, message = "使用量至少为1")
        @Schema(description = "使用量（图片：张数，视频：秒数）", example = "4")
        private Integer usageAmount;

        @Schema(description = "计费单位", example = "COUNT", 
                allowableValues = {"COUNT", "SECONDS"})
        private String billingUnit;

        @Schema(description = "媒体描述", example = "补充图片生成，粘入PPT中")
        private String description;
    }
}
