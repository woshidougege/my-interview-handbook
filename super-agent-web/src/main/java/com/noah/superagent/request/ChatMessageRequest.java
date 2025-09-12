package com.noah.superagent.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 聊天消息请求
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    
    /**
     * 工作空间ID
     */
    @NotNull(message = "工作空间ID不能为空")
    private String workspaceId;
    
    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空") 
    private String sessionId;
    
    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
    
    /**
     * 用户ID（可选，会从认证信息中获取）
     */
    private String userId;
}
