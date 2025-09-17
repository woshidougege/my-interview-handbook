package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.service.CreditDeductionTaskService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 积分扣减清理任务Job
 * <p>
 * 定时清理任务 - 每5分钟处理失败任务的重试
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class CreditDeductionTaskJob {

    private final CreditDeductionTaskService creditDeductionTaskService;
    
    // 清理任务名称
    private static final String CLEANUP_TASK_NAME = "credit-deduction-cleanup";
    
    // 每5分钟执行一次清理
    private static final String CLEANUP_CRON_EXPRESSION = "0 */5 * * * ?";
    
    // 每次处理的最大任务数
    private static final int MAX_TASKS_PER_BATCH = 20;
    
    // 超时任务重置阈值（分钟）
    private static final int TIMEOUT_MINUTES = 5;

    // 清理任务实例
    @Getter
    private final RecurringTask<Void> cleanupTask;

    public CreditDeductionTaskJob(CreditDeductionTaskService creditDeductionTaskService) {
        this.creditDeductionTaskService = creditDeductionTaskService;
        
        // 创建定时清理任务
        this.cleanupTask = Tasks.recurring(CLEANUP_TASK_NAME, Schedules.cron(CLEANUP_CRON_EXPRESSION))
                .execute(this::executeCleanup);
    }

    /**
     * 获取清理任务名称
     */
    public static String getCleanupTaskName() {
        return CLEANUP_TASK_NAME;
    }

    /**
     * 执行清理任务
     */
    private void executeCleanup(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        log.debug("开始执行积分扣减清理任务");
        
        try {
            // 1. 重置超时任务
            int resetCount = creditDeductionTaskService.resetTimeoutTasks(TIMEOUT_MINUTES, MAX_TASKS_PER_BATCH);
            if (resetCount > 0) {
                log.info("重置超时积分扣减任务数量: {}", resetCount);
            }
            
            // 2. 处理遗留的待处理任务（可能是系统重启前未处理的）
            int processedCount = creditDeductionTaskService.processPendingTasks(MAX_TASKS_PER_BATCH);
            if (processedCount > 0) {
                log.info("处理遗留积分扣减任务数量: {}", processedCount);
            }
            
        } catch (Exception e) {
            log.error("积分扣减清理任务异常", e);
            // 不重新抛出异常，避免影响调度器
        }
        
        log.debug("积分扣减清理任务完成");
    }
}
