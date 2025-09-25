package com.noah.superagent.common.dto.response;

import com.noah.superagent.common.enums.ChatTaskStatusEnum;
import com.noah.superagent.common.enums.FavoriteEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话任务响应DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "对话任务信息响应")
public class ChatTaskResponse extends BaseResponse {

    @Schema(description = "关联工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "会话ID，用于与下游平台通信", example = "context_001")
    private String contextId;

    @Schema(description = "对话任务标题", example = "技术问题咨询")
    private String title;

    @Schema(description = "对话任务内容", example = "如何使用Spring Boot开发Web应用？")
    private String content;

    @Schema(description = "是否收藏：FAVORITE-已收藏 NOT_FAVORITE-未收藏", example = "FAVORITE")
    private FavoriteEnum isFavorite;

    @Schema(description = "状态: IN_PROGRESS-进行中 COMPLETED-已完成 ARCHIVED-已归档", example = "IN_PROGRESS")
    private ChatTaskStatusEnum status;

    @Schema(description = "是否存在关联的定时任务", example = "true")
    private Boolean hasScheduledTask;

    @Schema(description = "关联的定时任务ID")
    private Long scheduledTaskId;

    @Schema(description = "原始聊天历史数据")
    private Object rawChatHistory;
}