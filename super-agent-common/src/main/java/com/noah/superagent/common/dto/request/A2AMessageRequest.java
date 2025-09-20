package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * A2A消息请求DTO
 */
@Data
@Schema(description = "A2A消息请求")
public class A2AMessageRequest {
    
    @Schema(description = "用户ID", example = "161312")
    private String userId;
    
    @Schema(description = "用户消息", example = "你好，帮我分析一下这个表格")
    private String message;
    
    @Schema(description = "会话ID", example = "sessionId_161312")
    private String sessionId;
}