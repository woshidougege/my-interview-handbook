package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.noah.superagent.service.CreditExpiryService;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 限时积分过期清理任务
 * <p>
 * 一次性延迟任务，为每个有限期的积分精确安排到期清理
 * 适用于有效期大于1天且小于永久的积分类型
 * <p>
 * 设计思路：
 * - 用户获得限时积分时，立即安排到期时间的清理任务
 * - 避免频繁轮询检查，提高效率
 * - 系统重启后任务自动恢复，保证可靠性
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class CreditExpiryCleanupTask {

    public static final String TASK_NAME = "credit-expiry-cleanup";
    
    private final CreditExpiryService creditExpiryService;
    
    @Getter
    private final OneTimeTask<CreditExpiryData> task;

    public CreditExpiryCleanupTask(CreditExpiryService creditExpiryService) {
        this.creditExpiryService = creditExpiryService;
        this.task = Tasks.oneTime(TASK_NAME, CreditExpiryData.class)
                .execute(this::processCreditExpiry);
        
        log.info("【限时积分过期清理Task】初始化完成");
    }

    /**
     * 处理单个用户的限时积分过期清理
     */
    private void processCreditExpiry(TaskInstance<CreditExpiryData> taskInstance, 
                                   ExecutionContext executionContext) {
        CreditExpiryData data = taskInstance.getData();
        String taskId = taskInstance.getId();
        
        log.info("【限时积分过期清理Task】开始执行 - taskId: {}, userId: {}, creditType: {}", 
                taskId, data.getUserId(), data.getCreditType());

        try {
            // 执行特定用户的积分过期清理
            boolean success = creditExpiryService.cleanupExpiredCreditsForUser(data.getUserId());
            
            if (success) {
                log.info("【限时积分过期清理Task】执行成功 - taskId: {}, userId: {}, creditType: {}", 
                        taskId, data.getUserId(), data.getCreditType());
            } else {
                log.warn("【限时积分过期清理Task】清理失败，但不影响系统运行 - taskId: {}, userId: {}", 
                        taskId, data.getUserId());
            }

        } catch (Exception e) {
            log.error("【限时积分过期清理Task】执行异常 - taskId: {}, userId: {}, error: {}", 
                    taskId, data.getUserId(), e.getMessage(), e);
            
            // 积分过期清理失败不抛异常，避免重试
            // 因为下一次定时清理会兜底处理
            log.info("积分过期清理异常已记录，系统将在下次定时清理中兜底处理");
        }
    }

    /**
     * 限时积分过期清理任务数据
     */
    @Data
    public static class CreditExpiryData {
        // Getters and Setters
        private Long userId;
        private String creditType;
        private java.math.BigDecimal amount;
        private java.time.LocalDateTime expiryTime;
        private String description;

        @Override
        public String toString() {
            return "CreditExpiryData{" +
                    "userId=" + userId +
                    ", creditType='" + creditType + '\'' +
                    ", amount=" + amount +
                    ", expiryTime=" + expiryTime +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}
