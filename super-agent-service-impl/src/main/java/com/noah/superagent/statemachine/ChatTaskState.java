package com.noah.superagent.statemachine;

import com.noah.superagent.dao.entity.ChatTaskEntity;

/**
 * 聊天任务状态接口
 */
public interface ChatTaskState {
    /**
     * 处理当前状态下的业务逻辑
     * @param context 状态上下文
     * @param task 聊天任务
     */
    void handle(ChatTaskContext context, ChatTaskEntity task);
    
    /**
     * 取消任务处理
     * @param context 状态上下文
     * @param task 聊天任务
     */
    default void cancel(ChatTaskContext context, ChatTaskEntity task) {
        // 默认实现，具体状态可以重写
        context.setState(new FailedState());
    }
    
    /**
     * 中断任务处理
     * @param context 状态上下文
     * @param task 聊天任务
     */
    default void interrupt(ChatTaskContext context, ChatTaskEntity task) {
        // 默认实现，具体状态可以重写
        context.setState(new FailedState());
    }
    
    /**
     * 处理任务失败
     * @param context 状态上下文
     * @param task 聊天任务
     * @param errorMessage 错误信息
     */
    default void fail(ChatTaskContext context, ChatTaskEntity task, String errorMessage) {
        // 默认实现，具体状态可以重写
        //task.setErrorMessage(errorMessage);
        context.setState(new FailedState());
    }
    
    /**
     * 前置处理
     * @param context 状态上下文
     * @param task 聊天任务
     */
    default void beforeHandle(ChatTaskContext context, ChatTaskEntity task) {
        // 默认空实现，具体状态可以重写
        System.out.println("Before handling task: " + task.getId() + " in state: " + getStateName());
    }
    
    /**
     * 后置处理
     * @param context 状态上下文
     * @param task 聊天任务
     */
    default void afterHandle(ChatTaskContext context, ChatTaskEntity task) {
        // 默认空实现，具体状态可以重写
        System.out.println("After handling task: " + task.getId() + " in state: " + getStateName());
    }
    
    /**
     * 获取状态名称
     * @return 状态名称
     */
    String getStateName();
}