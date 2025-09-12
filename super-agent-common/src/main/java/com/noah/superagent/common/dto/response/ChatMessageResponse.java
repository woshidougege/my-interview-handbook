package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话消息发送响应DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "对话消息发送响应")
public class ChatMessageResponse extends BaseResponse {

    @Schema(description = "对话任务ID")
    private Long chatTaskId;

    @Schema(description = "会话ID，用于与下游平台通信")
    private String contextId;

    @Schema(description = "AI响应内容")
    private String response;
}