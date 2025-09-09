package com.noah.superagent.schedule.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.service.CreditExpiryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 积分过期清理定时任务
 * <p>
 * 每天凌晨1:00执行，清理过期的积分
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Component
public class CreditExpiryCleanupJob {

    private final CreditExpiryService creditExpiryService;

    // 任务名称
    private static final String TASK_NAME = "credit-expiry-cleanup";
    
    // Cron表达式：每天凌晨01:00:00执行
    private static final String CRON_EXPRESSION = "0 0 1 * * ?";

    /**
     * -- GETTER --
     *  获取任务实例
     */
    // RecurringTask 实例
    @Getter
    private final RecurringTask<Void> task;

    public CreditExpiryCleanupJob(CreditExpiryService creditExpiryService) {
        this.creditExpiryService = creditExpiryService;
        // 按照官方文档使用 Tasks.recurring 创建重复任务
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(CRON_EXPRESSION))
                .execute(this::executeTask);
    }

    /**
     * 执行积分过期清理任务
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        log.info("开始执行积分过期清理任务 - taskInstance: {}", taskInstance.getId());
        
        try {
            // 清理过期的当日积分（1天前创建的）
            int dailyExpiredCount = creditExpiryService.cleanupExpiredDailyCredits();
            log.info("清理过期当日积分完成 - 处理用户数: {}", dailyExpiredCount);
            
            // 清理过期的活动积分（90天前创建的）
            int activityExpiredCount = creditExpiryService.cleanupExpiredActivityCredits();
            log.info("清理过期活动积分完成 - 处理用户数: {}", activityExpiredCount);
            
            // 清理过期的免费积分（90天前创建的）
            int freeExpiredCount = creditExpiryService.cleanupExpiredFreeCredits();
            log.info("清理过期免费积分完成 - 处理用户数: {}", freeExpiredCount);
            
            int totalProcessed = dailyExpiredCount + activityExpiredCount + freeExpiredCount;
            log.info("积分过期清理任务执行完成 - 总处理用户数: {}", totalProcessed);
            
        } catch (Exception e) {
            log.error("积分过期清理任务执行失败", e);
            throw e;
        }
    }

    /**
     * 获取任务名称
     */
    public static String getJobTaskName() {
        return TASK_NAME;
    }

    /**
     * 获取Cron表达式
     */
    public static String getCronExpression() {
        return CRON_EXPRESSION;
    }
}
