package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 定时对话任务创建请求DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "定时对话任务创建请求")
public class ScheduledChatTaskCreateRequest extends BaseRequest {

    @NotNull(message = "关联用户ID不能为空")
    @Schema(description = "关联用户ID", example = "1234567890123456789")
    private Long userId;

    @NotNull(message = "关联工作空间ID不能为空")
    @Schema(description = "关联工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "关联对话任务ID，为空表示新建对话任务", example = "1234567890123456789")
    private Long chatTaskId;

    @NotBlank(message = "任务名称不能为空")
    @Schema(description = "任务名称", example = "每日新闻摘要")
    private String taskName;

    @NotBlank(message = "Cron表达式不能为空")
    @Schema(description = "Cron表达式", example = "0 0 9 * * ?")
    private String cronExpression;

    @NotBlank(message = "对话提示词不能为空")
    @Schema(description = "对话提示词", example = "请为我生成今日科技新闻摘要")
    private String prompt;

    @Schema(description = "状态: 1启用 0禁用", example = "1")
    private Integer status = 1;
}