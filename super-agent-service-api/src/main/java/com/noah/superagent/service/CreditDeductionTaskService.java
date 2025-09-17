package com.noah.superagent.service;

import java.math.BigDecimal;

/**
 * 积分扣减任务服务接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface CreditDeductionTaskService {

    /**
     * 立即调度积分扣减任务（一次性任务）
     * @param userId 用户ID
     * @param amount 扣减金额
     * @param description 扣减描述
     * @param relatedOrderId 关联订单ID
     * @param resourceUsageRecordId 关联的资源使用记录ID
     * @return 任务ID
     */
    String scheduleImmediateCreditDeduction(Long userId, 
                                          BigDecimal amount, 
                                          String description, 
                                          Long relatedOrderId, 
                                          Long resourceUsageRecordId);

    /**
     * 更新任务状态为处理中
     * @param taskId 任务ID
     */
    void updateTaskToProcessing(String taskId);

    /**
     * 更新任务状态为成功
     * @param taskId 任务ID
     */
    void updateTaskToSuccess(String taskId);

    /**
     * 更新任务状态为失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void updateTaskToFailed(String taskId, String errorMessage);

    /**
     * 处理遗留的待处理任务（主要用于系统重启后的恢复）
     * @param maxTasks 最大处理任务数
     * @return 处理的任务数量
     */
    int processPendingTasks(int maxTasks);

    /**
     * 重置超时任务
     * @param timeoutMinutes 超时分钟数
     * @param maxTasks 最大重置任务数
     * @return 重置的任务数量
     */
    int resetTimeoutTasks(int timeoutMinutes, int maxTasks);
}
