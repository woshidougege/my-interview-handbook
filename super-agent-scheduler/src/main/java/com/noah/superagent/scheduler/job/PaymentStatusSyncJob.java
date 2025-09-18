package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.service.PaymentService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付状态同步兜底任务Job
 * <p>
 * 每天凌晨2点执行一次，作为最终兜底机制处理极端异常订单
 * 主要处理：db-scheduler任务异常、事件机制失败、系统长期宕机等极端情况
 * 
 * 注意：
 * - 90%订单通过微信回调实时处理
 * - 9%订单通过PaymentTimeoutCheckTask精确处理  
 * - 1%极端异常通过此Job兜底处理
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Component
public class PaymentStatusSyncJob {

    private final PaymentService paymentService;

    // 任务名称
    private static final String TASK_NAME = "payment-status-sync";
    
    // 每天凌晨2点执行一次，作为兜底机制
    private static final String CRON_EXPRESSION = "0 0 2 * * ?";

    // RecurringTask 实例
    @Getter
    private final RecurringTask<Void> task;

    public PaymentStatusSyncJob(PaymentService paymentService) {
        this.paymentService = paymentService;
        // 创建重复任务
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(CRON_EXPRESSION))
                .execute(this::executeTask);
    }

    /**
     * 获取任务名称
     */
    public static String getJobTaskName() {
        return TASK_NAME;
    }

    /**
     * 执行支付状态同步兜底任务
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("【支付状态同步兜底任务Job】开始执行 - 任务ID: {}", taskInstance.getId());
        
        try {
            // 执行状态同步
            int syncCount = paymentService.syncPendingOrdersStatus();
            
            long duration = System.currentTimeMillis() - startTime;
            if (syncCount > 0) {
                log.info("【支付状态同步兜底任务Job】执行完成 - 耗时: {}ms, 处理异常订单数: {}", duration, syncCount);
            } else {
                log.debug("【支付状态同步兜底任务Job】执行完成 - 耗时: {}ms, 无异常订单需要处理", duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【支付状态同步兜底任务Job】执行失败 - 耗时: {}ms, 错误: {}", 
                    duration, e.getMessage(), e);
            
            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("支付状态同步兜底任务Job执行失败: " + e.getMessage(), e);
        }
    }
}
