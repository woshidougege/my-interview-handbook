package com.noah.superagent.schedule.config;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.noah.superagent.schedule.job.DailyCreditBonusJob;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 调度器任务配置
 * 
 * 注册所有定时任务到db-scheduler
 *
 * @author Noah
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class SchedulerTaskConfig {

    private final DailyCreditBonusJob dailyCreditBonusJob;

    /**
     * 注册每日积分发放任务到db-scheduler
     */
    @Bean
    public RecurringTask<Void> dailyCreditBonusTask() {
        return dailyCreditBonusJob.getTask();
    }
}
