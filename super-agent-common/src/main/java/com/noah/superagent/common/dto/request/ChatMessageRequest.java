package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 对话消息发送请求DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "对话消息发送请求")
public class ChatMessageRequest extends BaseRequest {

    @NotNull(message = "工作空间ID不能为空")
    @Schema(description = "工作空间ID", example = "1234567890123456789")
    private Long workspaceId;

    @Schema(description = "对话任务ID，为空表示新建对话", example = "1234567890123456789")
    private Long chatTaskId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", example = "你好，请问有什么可以帮助你的吗？")
    private String message;
}