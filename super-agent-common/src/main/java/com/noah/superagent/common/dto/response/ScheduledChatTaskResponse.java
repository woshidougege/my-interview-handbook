package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 定时对话任务响应DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "定时对话任务响应")
public class ScheduledChatTaskResponse extends BaseResponse {

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "关联工作空间ID")
    private Long workspaceId;

    @Schema(description = "关联对话任务ID")
    private Long chatTaskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "对话提示词")
    private String prompt;

    @Schema(description = "状态: 1启用 0禁用")
    private Integer status;

    @Schema(description = "上次执行时间")
    private Date lastExecutionTime;

    @Schema(description = "下次执行时间")
    private Date nextExecutionTime;
}