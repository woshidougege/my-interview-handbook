package com.noah.superagent.common.dto;

import com.noah.superagent.common.enums.SseEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSE消息数据传输对象
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessageDto {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 事件类型
     */
    private SseEventType eventType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 工作空间ID
     */
    private String workspaceId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 错误信息（当eventType为ERROR时使用）
     */
    private String errorMessage;
    
    /**
     * 额外数据
     */
    private Object data;
}
