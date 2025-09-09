package com.noah.superagent.statemachine;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理中状态
 */
@Slf4j
public class ProcessingState implements ChatTaskState {
    
    @Override
    public void handle(ChatTaskContext context, ChatTaskEntity task) {
        log.info("处理进行中状态的任务: {}", task.getId());
        // 执行处理中状态的业务逻辑
        // 根据处理结果决定转换到完成或失败状态
        // 这里简单模拟处理结果
        if (Math.random() > 0.5) {
            context.setState(new CompletedState());
        } else if (task/*.getErrorMessage()*/ != null) {
            context.setState(new FailedState());
        }
        // 否则保持当前状态
    }
    
    @Override
    public void cancel(ChatTaskContext context, ChatTaskEntity task) {
        log.info("取消处理中状态的任务: {}", task.getId());
        //task.setErrorMessage("Task cancelled in processing state");
        context.setState(new FailedState());
    }
    
    @Override
    public void interrupt(ChatTaskContext context, ChatTaskEntity task) {
        log.info("中断处理中状态的任务: {}", task.getId());
        //task.setErrorMessage("Task interrupted in processing state");
        context.setState(new FailedState());
    }
    
    @Override
    public String getStateName() {
        return "PROCESSING";
    }
}