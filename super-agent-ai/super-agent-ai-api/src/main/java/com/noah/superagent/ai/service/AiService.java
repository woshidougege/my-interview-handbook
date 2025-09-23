package com.noah.superagent.ai.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI服务接口
 * 提供各种AI相关功能
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface AiService {

    /**
     * 生成聊天标题
     * 根据用户的第一个问题生成简洁的标题
     *
     * @param question 用户问题
     * @return 生成的标题
     */
    String generateChatTitle(String question);
    
    /**
     * 流式聊天
     * @param messages 聊天消息历史
     * @param resultCallback 结果回调，每个流式片段会调用一次
     * @param completeCallback 完成回调，流式处理完成时调用
     */
    void streamChat(List<ChatMessage> messages, Consumer<String> resultCallback, Runnable completeCallback);
    
    /**
     * 聊天消息
     */
    class ChatMessage {
        private String role; // system, user, assistant
        private String content;
        
        public ChatMessage() {}
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
