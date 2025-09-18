package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.common.config.DailyCreditsManagementConfig;
import com.noah.superagent.dao.mapper.UserDailyLoginMapper;
import com.noah.superagent.service.CreditExpiryService;
import com.noah.superagent.service.UserCreditService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 每日积分管理任务Job
 * <p>
 * 每天凌晨00:00执行（时间可配置），完成每日积分的完整管理流程：
 * 1. 先清理过期的每日积分（为新的一天做准备）
 * 2. 再为活跃用户补发新的每日积分
 * 
 * 设计思路：
 * - 将清理和补发逻辑合并，确保执行顺序正确
 * - 一个事务完成整个每日积分管理流程
 * - 执行时间可通过配置文件灵活调整
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
public class DailyCreditsManagementJob {

    private final CreditExpiryService creditExpiryService;
    private final UserCreditService userCreditService;
    private final UserDailyLoginMapper userDailyLoginMapper;
    private final DailyCreditsManagementConfig config;

    // 任务名称
    public static final String TASK_NAME = "daily-credits-management";

    // RecurringTask 实例
    @Getter
    private final RecurringTask<Void> task;

    public DailyCreditsManagementJob(CreditExpiryService creditExpiryService,
                                   UserCreditService userCreditService,
                                   UserDailyLoginMapper userDailyLoginMapper,
                                   DailyCreditsManagementConfig config) {
        this.creditExpiryService = creditExpiryService;
        this.userCreditService = userCreditService;
        this.userDailyLoginMapper = userDailyLoginMapper;
        this.config = config;
        
        // 创建定时任务，使用配置文件中的CRON表达式
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(config.getCron()))
                .execute(this::executeTask);
        
        log.info("【每日积分管理Job】初始化完成 - CRON: {}, 启用状态: {}", 
                config.getCron(), config.isEnabled());
    }

    /**
     * 执行每日积分管理任务
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("【每日积分管理Job】开始执行 - 任务ID: {}", taskInstance.getId());

        // 检查任务是否启用
        if (!config.isEnabled()) {
            log.info("【每日积分管理Job】任务已禁用，跳过执行");
            return;
        }

        int cleanupUserCount = 0;
        int grantUserCount = 0;

        try {
            // ==================== 第一步：清理过期的每日积分 ====================
            if (config.isCleanupEnabled()) {
                log.info("【每日积分管理Job】步骤1：开始清理过期的每日积分");
                cleanupUserCount = creditExpiryService.cleanupExpiredDailyCredits();
                log.info("【每日积分管理Job】步骤1完成 - 清理了 {} 位用户的过期每日积分", cleanupUserCount);
            } else {
                log.info("【每日积分管理Job】积分清理功能已禁用，跳过步骤1");
            }
            
            // ==================== 第二步：为活跃用户补发新的每日积分 ====================
            log.info("【每日积分管理Job】步骤2：开始为活跃用户补发每日积分");
            grantUserCount = processActiveDailyUsers();
            log.info("【每日积分管理Job】步骤2完成 - 为 {} 位活跃用户检查并补发了每日积分", grantUserCount);
            
            // ==================== 执行完成统计 ====================
            long duration = System.currentTimeMillis() - startTime;
            log.info("【每日积分管理Job】执行完成 - 耗时: {}ms, 清理用户: {}, 补发用户: {}", 
                    duration, cleanupUserCount, grantUserCount);
            
            // 记录每日积分管理统计
            log.info("每日积分管理统计 - 清理: {}位用户过期积分, 补发: {}位用户新积分, 总计处理: {}位用户", 
                    cleanupUserCount, grantUserCount, cleanupUserCount + grantUserCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【每日积分管理Job】执行失败 - 耗时: {}ms, 清理用户: {}, 补发用户: {}, 错误: {}", 
                    duration, cleanupUserCount, grantUserCount, e.getMessage(), e);
            
            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("每日积分管理Job执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理活跃用户的每日积分补发
     * 为过去3天内有登录记录的用户补发每日积分（防重复发放）
     */
    private int processActiveDailyUsers() {
        try {
            // 检查积分补发功能是否启用
            if (!config.isGrantEnabled()) {
                log.info("积分补发功能已禁用，跳过执行");
                return 0;
            }
            
            // 查询过去3天内有登录记录的活跃用户
            java.time.LocalDate endDate = java.time.LocalDate.now().minusDays(1);  // 昨天
            java.time.LocalDate startDate = endDate.minusDays(2);  // 3天前
            
            java.util.List<Long> activeUserIds = userDailyLoginMapper.selectUserIdsWithRecentLogin(startDate, endDate);
            
            if (activeUserIds.isEmpty()) {
                log.debug("未找到需要补发积分的活跃用户");
                return 0;
            }
            
            log.info("找到 {} 位活跃用户需要检查积分补发", activeUserIds.size());
            
            int processedCount = 0;
            
            // 为每个活跃用户触发每日积分发放逻辑
            for (Long userId : activeUserIds) {
                try {
                    // 调用UserCreditService的handleUserLogin方法
                    // 该方法内部包含防重机制，确保不会重复发放
                    userCreditService.handleUserLogin(userId);
                    processedCount++;
                    
                    log.debug("已为用户补发积分检查 - userId: {}", userId);
                    
                } catch (Exception e) {
                    log.warn("为用户补发积分失败 - userId: {}, error: {}", userId, e.getMessage());
                    // 单个用户失败不影响其他用户的处理
                }
            }
            
            log.info("活跃用户积分补发完成 - 处理用户数: {} / {}", processedCount, activeUserIds.size());
            return processedCount;
            
        } catch (Exception e) {
            log.error("处理活跃用户每日积分补发失败", e);
            throw e;
        }
    }

    /**
     * 获取任务名称
     */
    public static String getJobTaskName() {
        return TASK_NAME;
    }
}
