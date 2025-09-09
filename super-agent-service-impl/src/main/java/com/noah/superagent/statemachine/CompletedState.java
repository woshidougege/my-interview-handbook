package com.noah.superagent.statemachine;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * 已完成状态
 */
@Slf4j
public class CompletedState implements ChatTaskState {
    
    @Override
    public void handle(ChatTaskContext context, ChatTaskEntity task) {
        log.info("处理已完成状态的任务: {}", task.getId());
        // 执行已完成状态的业务逻辑
        // 已完成状态为终结状态，无需状态转换
    }
    
    @Override
    public void cancel(ChatTaskContext context, ChatTaskEntity task) {
        log.info("尝试取消已完成状态的任务: {}", task.getId());
        // 已完成的任务无法取消，记录日志即可
        log.warn("Cannot cancel completed task: {}", task.getId());
    }
    
    @Override
    public void interrupt(ChatTaskContext context, ChatTaskEntity task) {
        log.info("尝试中断已完成状态的任务: {}", task.getId());
        // 已完成的任务无法中断，记录日志即可
        log.warn("Cannot interrupt completed task: {}", task.getId());
    }
    
    @Override
    public String getStateName() {
        return "COMPLETED";
    }
}