package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话任务响应DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@Schema(description = "对话任务信息响应")
public class ChatTaskResponse {

    @Schema(description = "对话任务ID", example = "1234567890123456789")
    private Long id;

    @Schema(description = "关联工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "会话ID，用于与下游平台通信", example = "session_001")
    private String sessionId;

    @Schema(description = "对话任务标题", example = "技术问题咨询")
    private String title;

    @Schema(description = "对话任务内容", example = "如何使用Spring Boot开发Web应用？")
    private String content;

    @Schema(description = "是否收藏：1是 0否", example = "1")
    private Integer isFavorite;

    @Schema(description = "状态: 1进行中 2已完成 3已归档", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}