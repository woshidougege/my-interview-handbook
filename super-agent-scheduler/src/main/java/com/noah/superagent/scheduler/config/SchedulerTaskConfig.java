package com.noah.superagent.scheduler.config;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.noah.superagent.scheduler.job.PaymentStatusSyncJob;
import com.noah.superagent.scheduler.job.DailyCreditsManagementJob;
import com.noah.superagent.scheduler.job.ScheduledChatTaskJob;
import com.noah.superagent.scheduler.job.SubscriptionExpirationJob;
import com.noah.superagent.scheduler.task.CreditDeductionTask;
import com.noah.superagent.scheduler.task.CreditExpiryCleanupTask;
import com.noah.superagent.scheduler.task.PaymentTimeoutCheckTask;
import com.noah.superagent.scheduler.task.SubscriptionExpirationTask;
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
    
    /**
     * 注册订阅到期处理任务（一次性任务）
     */
    @Bean
    public OneTimeTask<SubscriptionExpirationTask.SubscriptionData> subscriptionExpirationTask(
            SubscriptionExpirationTask subscriptionExpirationTask) {
        return subscriptionExpirationTask.getTask();
    }
    
    // 注意: SubscriptionSchedulingTask 现在是事件监听器，不需要注册为Bean
    // 它会自动监听SubscriptionActivatedEvent和SubscriptionExtendedEvent
    
    /**
     * 注册每日积分管理任务（定期任务）
     * 包含清理过期积分和补发新积分的完整流程
     */
    @Bean
    public RecurringTask<Void> dailyCreditsManagementTask(DailyCreditsManagementJob dailyCreditsManagementJob) {
        return dailyCreditsManagementJob.getTask();
    }
    
    /**
     * 注册订阅到期处理任务Job（定期任务）
     * 作为兜底机制，主要处理通过OneTimeTask方式
     */
    @Bean
    public RecurringTask<Void> subscriptionExpirationJob(SubscriptionExpirationJob subscriptionExpirationJob) {
        return subscriptionExpirationJob.getTask();
    }
    
    /**
     * 注册支付超时检查任务（一次性任务）
     * 为每个订单精确安排超时检查，替代频繁轮询
     */
    @Bean
    public OneTimeTask<PaymentTimeoutCheckTask.PaymentTimeoutData> paymentTimeoutCheckTask(
            PaymentTimeoutCheckTask paymentTimeoutCheckTask) {
        return paymentTimeoutCheckTask.getTask();
    }
    
    /**
     * 注册限时积分过期清理任务（一次性延迟任务）
     * 为每个有限期的积分精确安排到期清理
     */
    @Bean
    public OneTimeTask<CreditExpiryCleanupTask.CreditExpiryData> creditExpiryCleanupTask(
            CreditExpiryCleanupTask creditExpiryCleanupTask) {
        return creditExpiryCleanupTask.getTask();
    }
    
    /**
     * 注册积分扣减任务（一次性立即任务）
     * 用于资源上报后立即执行积分扣减
     */
    @Bean
    public OneTimeTask<CreditDeductionTask.CreditDeductionData> creditDeductionTask(
            CreditDeductionTask creditDeductionTask) {
        return creditDeductionTask.getTask();
    }
}