package com.noah.superagent.scheduler.config;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.noah.superagent.scheduler.job.CreditExpiryCleanupJob;
import com.noah.superagent.scheduler.job.PaymentStatusSyncJob;
import com.noah.superagent.scheduler.job.ScheduledChatTaskJob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 调度任务配置
 * <p>
 * 将定时任务注册为Spring Bean，让db-scheduler能够自动发现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class SchedulerTaskConfig {

    /**
     * 注册积分过期清理任务
     */
    @Bean
    public RecurringTask<Void> creditExpiryCleanupTask(CreditExpiryCleanupJob creditExpiryCleanupJob) {
        return creditExpiryCleanupJob.getTask();
    }

    /**
     * 注册支付状态同步任务
     */
    @Bean
    public RecurringTask<Void> paymentStatusSyncTask(PaymentStatusSyncJob paymentStatusSyncJob) {
        return paymentStatusSyncJob.getTask();
    }
    
    /**
     * 注册定时对话任务
     */
    @Bean
    public RecurringTask<Void> scheduledChatTask(ScheduledChatTaskJob scheduledChatTaskJob) {
        return scheduledChatTaskJob.getTask();
    }
}