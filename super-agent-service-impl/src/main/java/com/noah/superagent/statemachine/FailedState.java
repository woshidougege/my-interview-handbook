package com.noah.superagent.statemachine;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * 已失败状态
 */
@Slf4j
public class FailedState implements ChatTaskState {
    
    @Override
    public void handle(ChatTaskContext context, ChatTaskEntity task) {
        log.info("处理已失败状态的任务: {}", task.getId());
        // 执行已失败状态的业务逻辑
        // 失败状态可以转换到重试或其他状态
    }
    
    @Override
    public void cancel(ChatTaskContext context, ChatTaskEntity task) {
        log.info("尝试取消已失败状态的任务: {}", task.getId());
        // 已失败的任务保持在失败状态
        log.warn("Task already failed, cannot cancel: {}", task.getId());
    }
    
    @Override
    public void interrupt(ChatTaskContext context, ChatTaskEntity task) {
        log.info("尝试中断已失败状态的任务: {}", task.getId());
        // 已失败的任务保持在失败状态
        log.warn("Task already failed, cannot interrupt: {}", task.getId());
    }
    
    @Override
    public void fail(ChatTaskContext context, ChatTaskEntity task, String errorMessage) {
        log.info("处理已失败状态的任务失败: {}", task.getId());
        // 更新错误信息
        //task.setErrorMessage(errorMessage);
        // 保持在失败状态
    }
    
    @Override
    public String getStateName() {
        return "FAILED";
    }
}