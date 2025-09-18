package com.noah.superagent.scheduler.task;

import com.noah.superagent.dao.mapper.UserDailyLoginMapper;
import com.noah.superagent.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日积分补发定时任务
 * <p>
 * 目标：为那些持续在线、未触发登录事件的用户补发每日积分。
 * 策略：每天凌晨 00:30 执行，扫描过去3天内有登录记录的用户，并为他们触发每日积分发放逻辑。
 * 防重：`UserCreditService.handleUserLogin` 方法内部包含防重机制，确保不会重复发放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCreditGrantTask {

    private final UserDailyLoginMapper userDailyLoginMapper;
    private final UserCreditService userCreditService;

    /**
     * 每天凌晨 00:00 执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void grantCreditsForActiveUsers() {
        log.info("【每日积分补发任务】开始执行...");

        try {
            // 1. 定义查询范围：今天 和 过去2天（总共3天）
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(2);
            log.info("【每日积分补发任务】查询范围: {} 到 {}", startDate, endDate);

            // 2. 查询最近3天内所有活跃过的用户ID
            List<Long> activeUserIds = userDailyLoginMapper.selectUserIdsWithRecentLogin(startDate, endDate);
            if (activeUserIds == null || activeUserIds.isEmpty()) {
                log.info("【每日积分补发任务】最近3天内没有活跃用户，任务结束。");
                return;
            }
            log.info("【每日积分补发任务】发现 {} 个活跃用户需要检查积分发放状态。", activeUserIds.size());

            // 3. 遍历用户，触发积分发放逻辑
            int successCount = 0;
            for (Long userId : activeUserIds) {
                try {
                    // 调用核心方法，该方法会记录登录并检查是否需要发放积分
                    // 方法内部有防重，所以重复调用是安全的
                    userCreditService.handleUserLogin(userId);
                    successCount++;
                } catch (Exception e) {
                    log.error("【每日积分补发任务】为用户 {} 处理积分时发生错误: {}", userId, e.getMessage());
                }
            }

            log.info("【每日积分补发任务】执行完成。成功处理用户数: {}", successCount);

        } catch (Exception e) {
            log.error("【每日积分补发任务】执行过程中发生严重错误: {}", e.getMessage(), e);
        }
    }
}
