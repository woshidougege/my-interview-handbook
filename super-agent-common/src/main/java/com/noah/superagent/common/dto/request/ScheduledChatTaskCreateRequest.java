package com.noah.superagent.common.dto.request;

import com.noah.superagent.common.dto.ScheduleConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 定时对话任务创建请求DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "定时对话任务创建请求")
public class ScheduledChatTaskCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1234567890123456789")
    private Long userId;

    @NotNull(message = "工作空间ID不能为空")
    @Schema(description = "工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "任务名称", example = "每日新闻摘要")
    private String taskName;

    @Deprecated
    @Schema(description = "Cron表达式 (已废弃，请使用scheduleConfig)")
    private String cronExpression;

    @NotNull(message = "任务调度配置不能为空")
    @Schema(description = "任务调度配置")
    private ScheduleConfig scheduleConfig;

    @NotBlank(message = "提示词不能为空")
    @Schema(description = "对话提示词", example = "请为我总结今天的科技新闻...")
    private String prompt;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 1启用 0禁用", example = "1")
    private Integer status;
    
    @NotNull(message = "任务类型不能为空")
    @Schema(description = "任务类型: 0-一次性任务 1-可重复任务", example = "1")
    private Integer taskType;
    

}