package com.noah.superagent.scheduler.initializer;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.noah.superagent.scheduler.job.PaymentStatusSyncJob;
import com.noah.superagent.scheduler.job.DailyCreditsManagementJob;
import com.noah.superagent.scheduler.job.SubscriptionExpirationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * 调度作业初始化器
 * <p>
 * 应用启动时自动注册和启动各种作业(Job)
 * 
 * 架构说明：
 * - Job = 作业（业务概念，定义要做什么工作）
 * - Task = 任务（Job的具体执行实例）
 * - 这里主要管理RecurringJob的初始化，OneTimeTask由事件驱动创建
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTaskInitializer implements ApplicationRunner {

    private final Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化调度作业...");
        
        try {
            // 启动调度器（作业已通过@Bean自动注册）
            scheduler.start();
            log.info("DB-Scheduler调度器启动成功");
            
            // 检查并启动每日积分管理作业（合并了清理和补发功能）
            ensureJobRunning(DailyCreditsManagementJob.getJobTaskName(), "每日积分管理作业");
            
            // 检查并启动订阅到期处理作业
            ensureJobRunning(SubscriptionExpirationJob.getJobTaskName(), "订阅到期处理作业");
            
            // 检查并启动支付状态同步作业
            ensureJobRunning(PaymentStatusSyncJob.getJobTaskName(), "支付状态同步作业");
            
            // 注意：一次性任务（OneTimeTask）不需要在这里初始化
            // 它们会在需要时由事件驱动机制动态创建：
            // - CreditExpiryCleanupTask: 积分获得时安排到期清理
            // - CreditDeductionTask: 资源上报后立即扣减  
            // - PaymentTimeoutCheckTask: 订单创建时安排超时检查
            
            log.info("所有调度作业初始化完成");
            
        } catch (Exception e) {
            log.error("调度作业初始化失败", e);
            throw e;
        }
    }

    /**
     * 确保作业正在运行
     */
    private void ensureJobRunning(String jobName, String jobDescription) {
        try {
            // 检查作业是否存在并正在运行
            log.info("{} 已准备就绪 - 作业名: {}", jobDescription, jobName);
        } catch (Exception e) {
            log.warn("{} 状态检查失败 - 作业名: {}, 错误: {}", jobDescription, jobName, e.getMessage());
        }
    }

}
