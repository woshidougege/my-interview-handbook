package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * A2A补充信息请求DTO
 */
@Data
@Schema(description = "A2A补充信息请求")
public class A2ASupplementInfoRequest {
    
    @Schema(description = "用户ID", example = "161312")
    private String userId;
    
    @Schema(description = "任务ID", example = "taskId")
    private String taskId;
    
    @Schema(description = "用户补充的信息", example = "小米第四季度财报")
    private String userInput;
    
    @Schema(description = "会话ID", example = "sessionId_161312")
    private String sessionId;
}