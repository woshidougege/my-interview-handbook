
package com.noah.superagent.common.dto.response;

import com.noah.superagent.common.dto.ScheduleConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 定时对话任务响应DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "定时对话任务响应")
public class ScheduledChatTaskResponse {

    @Schema(description = "定时任务ID", example = "1234567890123456789")
    private Long id;

    @Schema(description = "关联用户ID", example = "1234567890123456789")
    private Long userId;

    @Schema(description = "关联工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "关联对话任务ID", example = "1234567890123456789")
    private Long chatTaskId;

    @Schema(description = "任务名称", example = "每日新闻摘要")
    private String taskName;

    @Deprecated
    @Schema(description = "Cron表达式 (已废弃，将来会移除)")
    private String cronExpression;

    @Schema(description = "任务调度配置")
    private ScheduleConfig scheduleConfig;

    @Schema(description = "对话提示词")
    private String prompt;

    @Schema(description = "状态: 1启用 0禁用", example = "1")
    private Integer status;

    @Schema(description = "上次执行时间")
    private Date lastExecutionTime;

    @Schema(description = "下次执行时间")
    private Date nextExecutionTime;

    @Schema(description = "任务类型: 0-一次性任务 1-可重复任务", example = "1")
    private Integer taskType;

}