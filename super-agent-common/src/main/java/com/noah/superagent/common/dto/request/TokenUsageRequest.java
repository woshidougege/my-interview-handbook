package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Token使用量上报请求
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "Token使用量上报请求")
public class TokenUsageRequest {

    @NotBlank(message = "请求ID不能为空")
    @Schema(description = "请求ID（幂等键）", example = "req_20240101_agent123_session456_001")
    private String requestId;

    @NotBlank(message = "用户外部键不能为空")
    @Schema(description = "用户外部键", example = "user_67890")
    private String userExternalKey;

    @NotBlank(message = "智能体ID不能为空")
    @Schema(description = "智能体ID", example = "agent_12345")
    private String agentId;

    @Schema(description = "会话ID", example = "session_12345")
    private String sessionId;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "gpt-4")
    private String modelName;

    @NotNull(message = "输入Token数不能为空")
    @Min(value = 0, message = "输入Token数不能为负数")
    @Schema(description = "输入Token数量", example = "500")
    private Long inputTokens;

    @NotNull(message = "输出Token数不能为空")
    @Min(value = 0, message = "输出Token数不能为负数")
    @Schema(description = "输出Token数量", example = "300")
    private Long outputTokens;

    @Schema(description = "请求描述", example = "代码生成任务")
    private String description;
}
