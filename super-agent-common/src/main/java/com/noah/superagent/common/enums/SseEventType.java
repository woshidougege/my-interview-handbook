package com.noah.superagent.common.enums;

/**
 * SSE事件类型枚举
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
public enum SseEventType {
    
    /**
     * 连接建立
     */
    CONNECTED("connected"),
    
    /**
     * AI开始思考
     */
    AI_THINKING("ai_thinking"),
    
    /**
     * AI流式回复片段
     */
    AI_CHUNK("ai_chunk"),
    
    /**
     * AI回复完成
     */
    AI_COMPLETE("ai_complete"),
    
    /**
     * 错误信息
     */
    ERROR("error"),
    
    /**
     * 心跳
     */
    HEARTBEAT("heartbeat");
    
    private final String eventType;
    
    SseEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getEventType() {
        return eventType;
    }
}
