package com.noah.superagent.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息响应
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 发送者类型（user/assistant/system）
     */
    private String senderType;
    
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
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
