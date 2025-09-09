package com.noah.superagent.statemachine;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * 待处理状态
 */
@Slf4j
public class PendingState implements ChatTaskState {
    
    @Override
    public void handle(ChatTaskContext context, ChatTaskEntity task) {
        log.info("处理待处理状态的任务: {}", task.getId());
        // 执行待处理状态的业务逻辑
        // 状态转换到处理中
        context.setState(new ProcessingState());
    }
    
    @Override
    public void cancel(ChatTaskContext context, ChatTaskEntity task) {
        log.info("取消待处理状态的任务: {}", task.getId());
        //task.setErrorMessage("Task cancelled in pending state");
        context.setState(new FailedState());
    }
    
    @Override
    public void interrupt(ChatTaskContext context, ChatTaskEntity task) {
        log.info("中断待处理状态的任务: {}", task.getId());
        //task.setErrorMessage("Task interrupted in pending state");
        context.setState(new FailedState());
    }
    
    @Override
    public String getStateName() {
        return "PENDING";
    }
}