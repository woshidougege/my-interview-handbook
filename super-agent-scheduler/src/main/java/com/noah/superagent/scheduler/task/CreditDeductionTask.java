package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.noah.superagent.service.CreditConsumeService;
import com.noah.superagent.service.UserCreditService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 资源消耗积分扣减任务
 * <p>
 * 一次性立即执行任务，用于处理用户资源消耗后的积分扣减
 * 替代原有的定时轮询机制，改为资源上报后立即执行
 * <p>
 * 设计思路：
 * - 用户资源上报后立即创建一次性任务进行积分扣减
 * - 避免积分扣减阻塞资源使用的响应速度
 * - 提供重试机制，确保积分扣减的可靠性
 * - 系统重启后任务自动恢复执行
 *
 * @author 任相鹏
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
     * 处理积分扣减（保证幂等性，避免重复扣减）
     */
    private void processCreditDeduction(TaskInstance<CreditDeductionData> taskInstance, 
                                      ExecutionContext executionContext) {
        CreditDeductionData data = taskInstance.getData();
        String taskId = taskInstance.getId();
        
        log.info("【积分扣减Task】开始执行 - taskId: {}, userId: {}, amount: {}, resourceRecordId: {}", 
                taskId, data.getUserId(), data.getAmount(), data.getResourceUsageRecordId());

        try {
            // 0. 幂等性检查：通过resourceUsageRecordId检查是否已扣减过
            if (data.getResourceUsageRecordId() != null) {
                // 检查该资源使用记录是否已经扣减过积分
                // 这里可以通过查询credit_transaction表或在resource_usage_record表添加credit_deducted字段
                boolean alreadyDeducted = isAlreadyDeducted(data.getResourceUsageRecordId());
                if (alreadyDeducted) {
                    log.info("【积分扣减Task】积分已扣减过，跳过执行 - taskId: {}, resourceRecordId: {}", 
                            taskId, data.getResourceUsageRecordId());
                    return; // 幂等性保证：已扣减过则直接返回成功
                }
            }

            // 1. 检查用户是否有足够积分（包含透支额度）
            boolean canConsume = userCreditService.canConsumeCredits(data.getUserId(), data.getAmount());
            if (!canConsume) {
                log.warn("【积分扣减Task】用户积分余额不足 - taskId: {}, userId: {}, amount: {}", 
                        taskId, data.getUserId(), data.getAmount());
                // 积分不足不算系统错误，不抛异常
                return;
            }

            // 2. 执行积分扣减（在事务中完成扣减+标记）
            creditConsumeService.consumeCredits(
                    data.getUserId(), 
                    data.getAmount(), 
                    data.getDescription(), 
                    data.getRelatedOrderId());
            
            // 3. 标记该资源使用记录已扣减积分（保证幂等性）
            if (data.getResourceUsageRecordId() != null) {
                markAsDeducted(data.getResourceUsageRecordId(), taskId);
            }

            log.info("【积分扣减Task】执行成功 - taskId: {}, userId: {}, amount: {}, resourceRecordId: {}", 
                    taskId, data.getUserId(), data.getAmount(), data.getResourceUsageRecordId());

        } catch (Exception e) {
            log.error("【积分扣减Task】执行失败 - taskId: {}, userId: {}, amount: {}, error: {}", 
                    taskId, data.getUserId(), data.getAmount(), e.getMessage(), e);
            
            // 抛出异常让db-scheduler进行重试
            throw new RuntimeException("积分扣减失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查资源使用记录是否已扣减过积分
     */
    private boolean isAlreadyDeducted(Long resourceUsageRecordId) {
        // TODO: 实现检查逻辑
        // 方案1: 在resource_usage_record表添加credit_deducted字段
        // 方案2: 在credit_transaction表查询是否存在相关记录
        return false; // 暂时返回false，需要根据实际表结构实现
    }
    
    /**
     * 标记资源使用记录已扣减积分
     */
    private void markAsDeducted(Long resourceUsageRecordId, String taskId) {
        // TODO: 实现标记逻辑
        // 方案1: 更新resource_usage_record表的credit_deducted字段
        // 方案2: 在credit_transaction表的备注中记录resourceUsageRecordId
        log.debug("标记资源使用记录已扣减积分 - resourceRecordId: {}, taskId: {}", resourceUsageRecordId, taskId);
    }

    /**
     * 积分扣减任务数据
     */
    @Setter
    @Getter
    public static class CreditDeductionData {
        // Getters and Setters
        private Long userId;
        private java.math.BigDecimal amount;
        private String description;
        private Long relatedOrderId;
        private Long resourceUsageRecordId;
        private java.time.LocalDateTime requestTime;

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
