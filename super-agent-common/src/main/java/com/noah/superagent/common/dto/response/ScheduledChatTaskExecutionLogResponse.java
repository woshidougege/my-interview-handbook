package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 定时对话任务执行日志响应DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "定时对话任务执行日志响应")
public class ScheduledChatTaskExecutionLogResponse extends BaseResponse {

    @Schema(description = "定时任务ID", example = "1234567890123456789")
    private Long taskId;

    @Schema(description = "任务名称", example = "每日新闻摘要")
    private String taskName;

    @Schema(description = "执行开始时间")
    private Date startTime;

    @Schema(description = "执行结束时间")
    private Date endTime;

    @Schema(description = "执行状态: 1成功 0失败", example = "1")
    private Integer executionStatus;

    @Schema(description = "执行结果")
    private String executionResult;

    @Schema(description = "执行耗时(毫秒)", example = "1500")
    private Long duration;

    @Schema(description = "错误信息")
    private String errorMessage;
}