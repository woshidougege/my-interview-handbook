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
 * 支付状态同步定时任务
 * <p>
 * 每2分钟执行一次，检查处理中的订单状态
 * 防止回调通知丢失导致的状态不同步
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
    
    // 每2分钟执行一次
    private static final String CRON_EXPRESSION = "0 */2 * * * ?";

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
     * 执行支付状态同步任务
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.debug("开始执行支付状态同步任务 - 任务ID: {}", taskInstance.getId());
        
        try {
            // 执行状态同步
            int syncCount = paymentService.syncPendingOrdersStatus();
            
            long duration = System.currentTimeMillis() - startTime;
            if (syncCount > 0) {
                log.info("支付状态同步任务执行完成 - 耗时: {}ms, 同步订单数: {}", duration, syncCount);
            } else {
                log.debug("支付状态同步任务执行完成 - 耗时: {}ms, 无需同步的订单", duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("支付状态同步任务执行失败 - 耗时: {}ms, 错误: {}", 
                    duration, e.getMessage(), e);
            
            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("支付状态同步任务执行失败: " + e.getMessage(), e);
        }
    }
}
