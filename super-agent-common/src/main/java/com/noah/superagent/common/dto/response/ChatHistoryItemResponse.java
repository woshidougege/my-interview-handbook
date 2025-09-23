package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "聊天历史项响应")
public class ChatHistoryItemResponse {
    @Schema(description = "上下文ID")
    private String contextId;
    
    @Schema(description = "是否为最终结果")
    private Boolean finalResult;
    
    @Schema(description = "类型")
    private String kind;
    
    @Schema(description = "状态信息")
    private StatusInfo status;
    
    @Schema(description = "任务ID")
    private String taskId;
    
    @Schema(description = "消息信息")
    private MessageInfo message;
    
    @Data
    @Schema(description = "状态信息")
    public static class StatusInfo {
        @Schema(description = "消息")
        private MessageInfo message;
        
        @Schema(description = "状态")
        private String state;
        
        @Schema(description = "时间戳")
        private String timestamp;
    }
    
    @Data
    @Schema(description = "消息信息")
    public static class MessageInfo {
        @Schema(description = "上下文ID")
        private String contextId;
        
        @Schema(description = "类型")
        private String kind;
        
        @Schema(description = "消息ID")
        private String messageId;
        
        @Schema(description = "消息部分")
        private List<Object> parts;
        
        @Schema(description = "角色")
        private String role;
        
        @Schema(description = "任务ID")
        private String taskId;
    }
}