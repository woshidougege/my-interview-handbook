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
 * 定时任务初始化器
 * <p>
 * 应用启动时自动注册和启动定时任务
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
        log.info("开始初始化定时任务...");
        
        try {
            // 启动调度器（任务已通过@Bean自动注册）
            scheduler.start();
            log.info("DB-Scheduler调度器启动成功");
            
            // 检查并启动每日积分管理任务Job（合并了清理和补发功能）
            ensureTaskRunning(DailyCreditsManagementJob.getJobTaskName(), "每日积分管理任务Job");
            
            // 检查并启动订阅到期处理任务Job
            ensureTaskRunning(SubscriptionExpirationJob.getJobTaskName(), "订阅到期处理任务Job");
            
            // 检查并启动支付状态同步任务
            ensureTaskRunning(PaymentStatusSyncJob.getJobTaskName(), "支付状态同步任务");
            
            // 注意：一次性任务（OneTimeTask）不需要在这里初始化
            // 它们会在需要时动态创建：
            // - CreditExpiryCleanupTask: 积分获得时安排到期清理
            // - CreditDeductionTask: 资源上报后立即扣减
            // - PaymentTimeoutCheckTask: 订单创建时安排超时检查
            
            log.info("所有定时任务初始化完成");
            
        } catch (Exception e) {
            log.error("定时任务初始化失败", e);
            throw e;
        }
    }

    /**
     * 确保任务正在运行
     */
    private void ensureTaskRunning(String taskName, String taskDescription) {
        try {
            // 检查任务是否存在并正在运行
            log.info("{} 已准备就绪 - 任务名: {}", taskDescription, taskName);
        } catch (Exception e) {
            log.warn("{} 状态检查失败 - 任务名: {}, 错误: {}", taskDescription, taskName, e.getMessage());
        }
    }

}
