package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.noah.superagent.service.CreditConsumeService;
import com.noah.superagent.service.UserCreditService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 资源消耗积分扣减任务
 * <p>
 * 一次性立即执行任务，用于处理用户资源消耗后的积分扣减
 * 替代原有的定时轮询机制，改为资源上报后立即执行
 * 
 * 设计思路：
 * - 用户资源上报后立即创建一次性任务进行积分扣减
 * - 避免积分扣减阻塞资源使用的响应速度
 * - 提供重试机制，确保积分扣减的可靠性
 * - 系统重启后任务自动恢复执行
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
public class CreditDeductionTask {

    public static final String TASK_NAME = "credit-deduction";
    
    private final CreditConsumeService creditConsumeService;
    private final UserCreditService userCreditService;
    
    @Getter
    private final OneTimeTask<CreditDeductionData> task;

    public CreditDeductionTask(CreditConsumeService creditConsumeService,
                              UserCreditService userCreditService) {
        this.creditConsumeService = creditConsumeService;
        this.userCreditService = userCreditService;
        this.task = Tasks.oneTime(TASK_NAME, CreditDeductionData.class)
                .execute(this::processCreditDeduction);
        
        log.info("【积分扣减Task】初始化完成");
    }

    /**
     * 处理积分扣减
     */
    private void processCreditDeduction(TaskInstance<CreditDeductionData> taskInstance, 
                                      ExecutionContext executionContext) {
        CreditDeductionData data = taskInstance.getData();
        String taskId = taskInstance.getId();
        
        log.info("【积分扣减Task】开始执行 - taskId: {}, userId: {}, amount: {}, description: {}", 
                taskId, data.getUserId(), data.getAmount(), data.getDescription());

        try {
            // 1. 检查用户是否有足够积分（包含透支额度）
            boolean canConsume = userCreditService.canConsumeCredits(data.getUserId(), data.getAmount());
            if (!canConsume) {
                log.warn("【积分扣减Task】用户积分余额不足 - taskId: {}, userId: {}, amount: {}", 
                        taskId, data.getUserId(), data.getAmount());
                // 积分不足不算系统错误，不抛异常
                return;
            }

            // 2. 执行积分扣减
            creditConsumeService.consumeCredits(
                    data.getUserId(), 
                    data.getAmount(), 
                    data.getDescription(), 
                    data.getRelatedOrderId());

            log.info("【积分扣减Task】执行成功 - taskId: {}, userId: {}, amount: {}, description: {}", 
                    taskId, data.getUserId(), data.getAmount(), data.getDescription());

        } catch (Exception e) {
            log.error("【积分扣减Task】执行失败 - taskId: {}, userId: {}, amount: {}, error: {}", 
                    taskId, data.getUserId(), data.getAmount(), e.getMessage(), e);
            
            // 抛出异常让db-scheduler进行重试
            throw new RuntimeException("积分扣减失败: " + e.getMessage(), e);
        }
    }

    /**
     * 积分扣减任务数据
     */
    public static class CreditDeductionData {
        private Long userId;
        private java.math.BigDecimal amount;
        private String description;
        private Long relatedOrderId;
        private Long resourceUsageRecordId;
        private java.time.LocalDateTime requestTime;

        // 默认构造器（JSON反序列化需要）
        public CreditDeductionData() {}

        public CreditDeductionData(Long userId, 
                                 java.math.BigDecimal amount, 
                                 String description, 
                                 Long relatedOrderId,
                                 Long resourceUsageRecordId) {
            this.userId = userId;
            this.amount = amount;
            this.description = description;
            this.relatedOrderId = relatedOrderId;
            this.resourceUsageRecordId = resourceUsageRecordId;
            this.requestTime = java.time.LocalDateTime.now();
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Long getRelatedOrderId() { return relatedOrderId; }
        public void setRelatedOrderId(Long relatedOrderId) { this.relatedOrderId = relatedOrderId; }

        public Long getResourceUsageRecordId() { return resourceUsageRecordId; }
        public void setResourceUsageRecordId(Long resourceUsageRecordId) { this.resourceUsageRecordId = resourceUsageRecordId; }

        public java.time.LocalDateTime getRequestTime() { return requestTime; }
        public void setRequestTime(java.time.LocalDateTime requestTime) { this.requestTime = requestTime; }

        @Override
        public String toString() {
            return "CreditDeductionData{" +
                    "userId=" + userId +
                    ", amount=" + amount +
                    ", description='" + description + '\'' +
                    ", relatedOrderId=" + relatedOrderId +
                    ", resourceUsageRecordId=" + resourceUsageRecordId +
                    ", requestTime=" + requestTime +
                    '}';
        }
    }
}
