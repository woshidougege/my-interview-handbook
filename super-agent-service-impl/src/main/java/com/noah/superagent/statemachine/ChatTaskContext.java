package com.noah.superagent.statemachine;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import org.springframework.stereotype.Component;

/**
 * 聊天任务状态上下文
 */
@Component
public class ChatTaskContext {
    private ChatTaskState currentState;
    
    public ChatTaskContext() {
        // 初始状态为待处理
        this.currentState = new PendingState();
    }
    
    public void setState(ChatTaskState state) {
        this.currentState = state;
    }
    
    public ChatTaskState getCurrentState() {
        return currentState;
    }
    
    public void handle(ChatTaskEntity task) {
        currentState.beforeHandle(this, task);
        currentState.handle(this, task);
        currentState.afterHandle(this, task);
    }
    
    public void cancel(ChatTaskEntity task) {
        currentState.beforeHandle(this, task);
        currentState.cancel(this, task);
        currentState.afterHandle(this, task);
    }
    
    public void interrupt(ChatTaskEntity task) {
        currentState.beforeHandle(this, task);
        currentState.interrupt(this, task);
        currentState.afterHandle(this, task);
    }
    
    public void fail(ChatTaskEntity task, String errorMessage) {
        currentState.beforeHandle(this, task);
        currentState.fail(this, task, errorMessage);
        currentState.afterHandle(this, task);
    }
}