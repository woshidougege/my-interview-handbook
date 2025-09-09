package com.noah.superagent.schedule.initializer;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.noah.superagent.schedule.job.CreditExpiryCleanupJob;
import com.noah.superagent.schedule.job.DailyCreditBonusJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * 定时任务初始化器
 * 
 * 应用启动时自动注册和启动定时任务
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskInitializer implements ApplicationRunner {

    private final Scheduler scheduler;
    private final DailyCreditBonusJob dailyCreditBonusJob;
    private final CreditExpiryCleanupJob creditExpiryCleanupJob;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化定时任务...");
        
        try {
            // 启动调度器
            scheduler.start();
            log.info("DB-Scheduler调度器启动成功");
            
            // 注册每日积分发放任务
            initDailyCreditBonusTask();
            
            // 注册积分过期清理任务
            initCreditExpiryCleanupTask();
            
            log.info("所有定时任务初始化完成");
            
        } catch (Exception e) {
            log.error("定时任务初始化失败", e);
            throw e;
        }
    }

    /**
     * 初始化每日积分发放任务
     */
    private void initDailyCreditBonusTask() {
        try {
            String taskName = DailyCreditBonusJob.getJobTaskName();
            
            // 对于RecurringTask，使用schedule启动重复任务，任务会按照cron表达式自动重复执行
            scheduler.schedule(
                    dailyCreditBonusJob.getTask().instance(taskName),
                    java.time.Instant.now()
            );
            log.info("每日积分发放任务注册成功 - 任务名: {}, Cron: {}", taskName, DailyCreditBonusJob.getCronExpression());
                    
        } catch (Exception e) {
            log.error("初始化每日积分发放任务失败", e);
            throw new RuntimeException("初始化每日积分发放任务失败", e);
        }
    }

    /**
     * 初始化积分过期清理任务
     */
    private void initCreditExpiryCleanupTask() {
        try {
            String taskName = CreditExpiryCleanupJob.getJobTaskName();
            
            // 对于RecurringTask，使用schedule启动重复任务，任务会按照cron表达式自动重复执行
            scheduler.schedule(
                    creditExpiryCleanupJob.getTask().instance(taskName),
                    java.time.Instant.now()
            );
            log.info("积分过期清理任务注册成功 - 任务名: {}, Cron: {}", taskName, CreditExpiryCleanupJob.getCronExpression());
                    
        } catch (Exception e) {
            log.error("初始化积分过期清理任务失败", e);
            throw new RuntimeException("初始化积分过期清理任务失败", e);
        }
    }
}
