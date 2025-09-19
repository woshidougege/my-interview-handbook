package com.noah.superagent.scheduler.config;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.noah.superagent.scheduler.job.PaymentStatusSyncJob;
import com.noah.superagent.scheduler.job.DailyCreditsManagementJob;
import com.noah.superagent.scheduler.job.SubscriptionExpirationJob;
import com.noah.superagent.scheduler.task.CreditDeductionTask;
import com.noah.superagent.scheduler.task.CreditExpiryCleanupTask;
import com.noah.superagent.scheduler.task.PaymentTimeoutCheckTask;
import com.noah.superagent.scheduler.task.ScheduledChatExecutionTask;
import com.noah.superagent.scheduler.task.ScheduledChatTaskSchedulingTask;
import com.noah.superagent.scheduler.task.SubscriptionExpirationTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 调度作业配置
 * <p>
 * 将各种作业(Job)注册为Spring Bean，让db-scheduler能够自动发现和管理
 * <p>
 * 架构说明：
 * - Job = 作业（业务概念，定义要做什么工作）
 * - Task = 任务（Job的具体执行实例）
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class SchedulerJobConfig {

    // ==================== 定期作业 (RecurringJob) ====================
    
    /**
     * 注册支付状态同步Task
     */
    @Bean
    public RecurringTask<Void> paymentStatusSyncTask(PaymentStatusSyncJob paymentStatusSyncJob) {
        return paymentStatusSyncJob.getTask();
    }
    
    /**
     * 注册定时聊天执行Task（一次性Task）
     * 每个定时聊天任务会创建独立的执行实例，精确在指定时间执行
     */
    @Bean
    public OneTimeTask<ScheduledChatExecutionTask.ChatTaskData> scheduledChatExecutionTask(
            ScheduledChatExecutionTask scheduledChatExecutionTask) {
        return scheduledChatExecutionTask.getTask();
    }

    /**
     * 配置定时聊天任务的循环依赖关系
     * 解决ScheduledChatExecutionTask和ScheduledChatTaskSchedulingTask之间的循环依赖
     */
    @Bean
    public ScheduledChatTaskSchedulingTask scheduledChatTaskSchedulingTask(
            ScheduledChatExecutionTask scheduledChatExecutionTask,
            ScheduledChatTaskSchedulingTask schedulingTask) {
        // 设置循环依赖
        scheduledChatExecutionTask.setSchedulingTask(schedulingTask);
        return schedulingTask;
    }
    
    /**
     * 注册每日积分管理Task（定期Task）
     * 包含清理过期积分和补发新积分的完整流程
     */
    @Bean
    public RecurringTask<Void> dailyCreditsManagementTask(DailyCreditsManagementJob dailyCreditsManagementJob) {
        return dailyCreditsManagementJob.getTask();
    }
    
    /**
     * 注册订阅到期处理Task（定期Task）
     * 作为兜底机制，主要处理通过OneTimeTask方式
     */
    @Bean
    public RecurringTask<Void> subscriptionExpirationRecurringTask(SubscriptionExpirationJob subscriptionExpirationJob) {
        return subscriptionExpirationJob.getTask();
    }
    
    // ==================== 一次性作业 (OneTimeJob) ====================
    
    /**
     * 注册订阅到期处理Task（一次性Task）
     * 精确处理单个订阅的到期逻辑
     */
    @Bean
    public OneTimeTask<SubscriptionExpirationTask.SubscriptionData> subscriptionExpirationOneTimeTask(
            SubscriptionExpirationTask subscriptionExpirationTask) {
        return subscriptionExpirationTask.getTask();
    }
    
    /**
     * 注册支付超时检查Task（一次性Task）
     * 为每个订单精确安排超时检查，替代频繁轮询
     */
    @Bean
    public OneTimeTask<PaymentTimeoutCheckTask.PaymentTimeoutData> paymentTimeoutCheckOneTimeTask(
            PaymentTimeoutCheckTask paymentTimeoutCheckTask) {
        return paymentTimeoutCheckTask.getTask();
    }
    
    /**
     * 注册限时积分过期清理Task（一次性延迟Task）
     * 为每个有限期的积分精确安排到期清理
     */
    @Bean
    public OneTimeTask<CreditExpiryCleanupTask.CreditExpiryData> creditExpiryCleanupOneTimeTask(
            CreditExpiryCleanupTask creditExpiryCleanupTask) {
        return creditExpiryCleanupTask.getTask();
    }
    
    /**
     * 注册积分扣减Task（一次性立即Task）
     * 用于资源上报后立即执行积分扣减
     */
    @Bean
    public OneTimeTask<CreditDeductionTask.CreditDeductionData> creditDeductionOneTimeTask(
            CreditDeductionTask creditDeductionTask) {
        return creditDeductionTask.getTask();
    }
    
    // 注意: 
    // - SubscriptionSchedulingTask 和 PaymentSchedulingTask 是事件监听器，不需要注册为Bean
    // - 它们会自动监听对应的事件并创建一次性任务
}