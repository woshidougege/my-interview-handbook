package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 资源使用量上报响应
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "资源使用量上报响应")
public class ResourceUsageResponse {

    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "响应消息", example = "上报成功")
    private String message;

    @Schema(description = "报告ID", example = "report_12345")
    private String reportId;

    @Schema(description = "计费金额", example = "1.23")
    private BigDecimal billingAmount;

    @Schema(description = "积分扣减", example = "50")
    private BigDecimal creditDeducted;

    @Schema(description = "积分余额", example = "950")
    private BigDecimal creditBalance;

    /**
     * 创建成功响应
     */
    public static ResourceUsageResponse success(String reportId) {
        ResourceUsageResponse response = new ResourceUsageResponse();
        response.setSuccess(true);
        response.setMessage("上报成功");
        response.setReportId(reportId);
        return response;
    }

    /**
     * 创建成功响应（包含计费信息）
     */
    public static ResourceUsageResponse success(String reportId, BigDecimal billingAmount, 
            BigDecimal creditDeducted, BigDecimal creditBalance) {
        ResourceUsageResponse response = new ResourceUsageResponse();
        response.setSuccess(true);
        response.setMessage("上报成功");
        response.setReportId(reportId);
        response.setBillingAmount(billingAmount);
        response.setCreditDeducted(creditDeducted);
        response.setCreditBalance(creditBalance);
        return response;
    }

    /**
     * 创建重复请求响应
     */
    public static ResourceUsageResponse duplicate(String reportId) {
        ResourceUsageResponse response = new ResourceUsageResponse();
        response.setSuccess(true);
        response.setMessage("重复请求，已处理");
        response.setReportId(reportId);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static ResourceUsageResponse failed(String message) {
        ResourceUsageResponse response = new ResourceUsageResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
