package com.noah.superagent.schedule.initializer;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.noah.superagent.schedule.job.DailyCreditBonusJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 定时任务初始化器
 * 
 * 应用启动时自动注册和启动定时任务
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskInitializer implements ApplicationRunner {

    private final Scheduler scheduler;
    private final DailyCreditBonusJob dailyCreditBonusJob;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化定时任务...");
        
        try {
            // 启动调度器
            scheduler.start();
            log.info("DB-Scheduler调度器启动成功");
            
            // 注册每日积分发放任务
            initDailyCreditBonusTask();
            
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
            // 检查任务是否已经存在
            String taskName = DailyCreditBonusJob.getJobTaskName();
            
            // 计算下次执行时间（明天凌晨00:00:30）
            LocalDateTime nextRun = LocalDateTime.now()
                    .plusDays(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(30)
                    .withNano(0);
            
            // 如果今天还没有执行过，且当前时间在00:00:30之后，则立即执行一次
            LocalDateTime today00h00m30s = LocalDateTime.now()
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(30)
                    .withNano(0);
            
            if (LocalDateTime.now().isAfter(today00h00m30s)) {
                // 今天已经过了执行时间，使用明天的时间
                log.info("今日积分发放时间已过，下次执行时间: {}", nextRun);
            } else {
                // 今天还没到执行时间，使用今天的时间
                nextRun = today00h00m30s;
                log.info("今日积分发放时间未到，下次执行时间: {}", nextRun);
            }
            
            // 调度任务（如果任务已经存在，会自动更新执行时间）
            scheduler.schedule(
                    dailyCreditBonusJob.getTask().instance(taskName),
                    nextRun.atZone(ZoneId.systemDefault()).toInstant()
            );
            
            log.info("每日积分发放任务调度成功 - 任务名: {}, Cron: {}, 下次执行: {}", 
                    taskName, DailyCreditBonusJob.getCronExpression(), nextRun);
                    
        } catch (Exception e) {
            log.error("初始化每日积分发放任务失败", e);
            throw new RuntimeException("初始化每日积分发放任务失败", e);
        }
    }
}
