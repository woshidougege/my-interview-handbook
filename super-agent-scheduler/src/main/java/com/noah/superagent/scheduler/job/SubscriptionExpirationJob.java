package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.service.UserSubscriptionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订阅到期处理任务Job
 * <p>
 * 定期检查并处理到期的订阅，执行降级逻辑
 * 每小时执行一次，确保订阅到期及时处理
 * 
 * 注意：这是一个兜底机制，主要的订阅到期处理通过事件驱动的OneTimeTask完成
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
public class SubscriptionExpirationJob {

    private final UserSubscriptionService userSubscriptionService;

    // 任务名称
    private static final String TASK_NAME = "subscription-expiration-job";
    
    // 每小时执行一次，作为兜底机制
    private static final String CRON_EXPRESSION = "0 0 * * * ?";

    // RecurringTask 实例
    @Getter
    private final RecurringTask<Void> task;

    public SubscriptionExpirationJob(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
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
     * 执行订阅到期处理任务
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("【订阅到期处理任务Job】开始执行 - 任务ID: {}", taskInstance.getId());
        
        try {
            // 处理到期的订阅（兜底机制）
            int processedCount = userSubscriptionService.processSubscriptionExpirations();
            
            long duration = System.currentTimeMillis() - startTime;
            if (processedCount > 0) {
                log.info("【订阅到期处理任务Job】执行完成 - 耗时: {}ms, 处理订阅数: {}", duration, processedCount);
            } else {
                log.debug("【订阅到期处理任务Job】执行完成 - 耗时: {}ms, 无需处理的订阅", duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【订阅到期处理任务Job】执行失败 - 耗时: {}ms, 错误: {}", 
                    duration, e.getMessage(), e);
            
            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("订阅到期处理任务Job执行失败: " + e.getMessage(), e);
        }
    }
}
