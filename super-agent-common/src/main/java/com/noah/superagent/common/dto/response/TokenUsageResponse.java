package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Token使用量上报响应
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "Token使用量上报响应")
public class TokenUsageResponse {

    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "响应消息", example = "上报成功")
    private String message;

    @Schema(description = "报告ID", example = "report_12345")
    private String reportId;

    /**
     * 创建成功响应
     */
    public static TokenUsageResponse success(String reportId) {
        TokenUsageResponse response = new TokenUsageResponse();
        response.setSuccess(true);
        response.setMessage("上报成功");
        response.setReportId(reportId);
        return response;
    }

    /**
     * 创建重复请求响应
     */
    public static TokenUsageResponse duplicate(String reportId) {
        TokenUsageResponse response = new TokenUsageResponse();
        response.setSuccess(true);
        response.setMessage("重复请求，已处理");
        response.setReportId(reportId);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static TokenUsageResponse failed(String message) {
        TokenUsageResponse response = new TokenUsageResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
