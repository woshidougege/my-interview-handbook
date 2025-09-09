package com.noah.superagent.schedule.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.service.UserCreditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 每日免费套餐积分发放定时任务
 * 
 * 每日凌晨00:00:30执行，给所有免费套餐用户发放300积分
 * 使用db-scheduler确保高可靠性，避免程序重启导致的调度失效
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Component
public class DailyCreditBonusJob {

    private static final Logger log = LoggerFactory.getLogger(DailyCreditBonusJob.class);
    
    private final UserCreditService userCreditService;

    // 任务名称
    private static final String TASK_NAME = "daily-credit-bonus";
    
    // Cron表达式：每天凌晨00:00:00执行
    private static final String CRON_EXPRESSION = "0 0 0 * * ?";
    
    // RecurringTask 实例
    private final RecurringTask<Void> task;

    public DailyCreditBonusJob(UserCreditService userCreditService) {
        this.userCreditService = userCreditService;
        // 按照官方文档使用 Tasks.recurring 创建重复任务
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(CRON_EXPRESSION))
                .execute(this::executeTask);
    }
    
    public RecurringTask<Void> getTask() {
        return task;
    }

    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("开始执行每日免费套餐积分发放任务 - 任务ID: {}", taskInstance.getId());
        
        try {
            // 执行批量积分发放
            String result = userCreditService.processFreePlanDailyBonusForAllUsers();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("每日免费套餐积分发放任务执行成功 - 耗时: {}ms, 结果: {}", duration, result);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("每日免费套餐积分发放任务执行失败 - 耗时: {}ms, 错误: {}", 
                    duration, e.getMessage(), e);
            
            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("每日积分发放任务执行失败: " + e.getMessage(), e);
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
