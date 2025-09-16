package com.noah.superagent.common.dto.request;

import com.noah.superagent.common.dto.ScheduleConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时对话任务更新请求DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "定时对话任务更新请求")
public class ScheduledChatTaskUpdateRequest extends BaseRequest {

    @Schema(description = "关联对话任务ID", example = "1234567890123456789")
    private Long chatTaskId;

    @Schema(description = "任务名称", example = "每日新闻摘要")
    private String taskName;

    @Deprecated
    @Schema(description = "Cron表达式 (已废弃，请使用scheduleConfig)")
    private String cronExpression;

    @Schema(description = "任务调度配置")
    private ScheduleConfig scheduleConfig;

    @Schema(description = "对话提示词", example = "请为我生成今日科技新闻摘要")
    private String prompt;

    @Schema(description = "状态: 1启用 0禁用", example = "1")
    private Integer status;
}