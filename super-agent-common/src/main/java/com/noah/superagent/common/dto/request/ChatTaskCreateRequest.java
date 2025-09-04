package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 对话任务创建请求DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "对话任务创建请求")
public class ChatTaskCreateRequest extends BaseRequest {

    @NotNull(message = "关联工作空间ID不能为空")
    @Schema(description = "关联工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "会话ID，用于与下游平台通信", example = "session_001")
    private String sessionId;

    @NotBlank(message = "对话任务标题不能为空")
    @Schema(description = "对话任务标题", example = "技术问题咨询")
    private String title;

    @Schema(description = "对话任务内容", example = "如何使用Spring Boot开发Web应用？")
    private String content;

    @Schema(description = "是否收藏：1是 0否", example = "1")
    private Integer isFavorite;

    @Schema(description = "状态: 1进行中 2已完成 3已归档", example = "1")
    private Integer status;
}