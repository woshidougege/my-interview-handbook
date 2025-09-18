package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.common.config.DailyCreditGrantConfig;
import com.noah.superagent.dao.mapper.UserDailyLoginMapper;
import com.noah.superagent.service.UserCreditService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日积分补发任务Job
 * <p>
 * 目标：为那些持续在线、未触发登录事件的用户补发每日积分。
 * 策略：每天凌晨执行（时间可配置），扫描过去3天内有登录记录的用户，并为他们触发每日积分发放逻辑。
 * 防重：`UserCreditService.handleUserLogin` 方法内部包含防重机制，确保不会重复发放。
 * 
 * 使用 db-scheduler 确保任务的可靠性，程序挂了重启后会自动补执行
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
public class DailyCreditGrantJob {

    private static final String TASK_NAME = "daily-credit-grant";
    
    private final UserDailyLoginMapper userDailyLoginMapper;
    private final UserCreditService userCreditService;
    private final DailyCreditGrantConfig config;
    
    @Getter
    private final RecurringTask<Void> task;

    public DailyCreditGrantJob(UserDailyLoginMapper userDailyLoginMapper, 
                               UserCreditService userCreditService,
                               DailyCreditGrantConfig config) {
        this.userDailyLoginMapper = userDailyLoginMapper;
        this.userCreditService = userCreditService;
        this.config = config;
        
        // 创建db-scheduler重复任务，使用配置文件中的CRON表达式
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(config.getCron()))
                .execute(this::executeTask);
        
        log.info("每日积分补发任务Job已初始化 - CRON: {}, 启用状态: {}", config.getCron(), config.isEnabled());
    }

    /**
     * 获取任务名称（用于初始化器）
     */
    public static String getJobTaskName() {
        return TASK_NAME;
    }

    /**
     * 执行每日积分补发任务
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("【每日积分补发任务Job】开始执行 - 任务ID: {}, CRON: {}", 
                taskInstance.getId(), config.getCron());

        // 检查任务是否启用
        if (!config.isEnabled()) {
            log.info("【每日积分补发任务Job】任务已禁用，跳过执行");
            return;
        }

        try {
            // 1. 定义查询范围：今天 和 过去2天（总共3天）
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(2);
            log.info("【每日积分补发任务Job】查询范围: {} 到 {}", startDate, endDate);

            // 2. 查询最近3天内所有活跃过的用户ID
            List<Long> activeUserIds = userDailyLoginMapper.selectUserIdsWithRecentLogin(startDate, endDate);
            if (activeUserIds == null || activeUserIds.isEmpty()) {
                log.info("【每日积分补发任务Job】最近3天内没有活跃用户，任务结束。");
                return;
            }
            log.info("【每日积分补发任务Job】发现 {} 个活跃用户需要检查积分发放状态。", activeUserIds.size());

            // 3. 遍历用户，触发积分发放逻辑
            int successCount = 0;
            for (Long userId : activeUserIds) {
                try {
                    // 调用核心方法，该方法会记录登录并检查是否需要发放积分
                    // 方法内部有防重，所以重复调用是安全的
                    userCreditService.handleUserLogin(userId);
                    successCount++;
                } catch (Exception e) {
                    log.error("【每日积分补发任务Job】为用户 {} 处理积分时发生错误: {}", userId, e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("【每日积分补发任务Job】执行完成 - 耗时: {}ms, 成功处理用户数: {}", duration, successCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【每日积分补发任务Job】执行失败 - 耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            
            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("每日积分补发任务Job执行失败: " + e.getMessage(), e);
        }
    }
}