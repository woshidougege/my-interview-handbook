package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.noah.superagent.service.UserSubscriptionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订阅到期处理任务
 * <p>
 * 使用 db-scheduler 的 OneTimeTask 实现精确的延迟任务调度
 * 每个订阅在激活时会安排一个到期任务，到期时精确处理降级逻辑
 */
@Slf4j
@Component
public class SubscriptionExpirationTask {

    public static final String TASK_NAME = "subscription-expiration";
    
    private final UserSubscriptionService userSubscriptionService;
    
    @Getter
    private final OneTimeTask<SubscriptionData> task;

    public SubscriptionExpirationTask(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
        
        // 创建一次性任务，用于处理订阅到期
        this.task = Tasks.oneTime(TASK_NAME, SubscriptionData.class)
                .execute(this::processSubscriptionExpiration);
    }

    /**
     * 处理单个订阅到期
     */
    private void processSubscriptionExpiration(TaskInstance<SubscriptionData> taskInstance, 
                                             ExecutionContext executionContext) {
        SubscriptionData data = taskInstance.getData();
        Long subscriptionId = data.getSubscriptionId();
        Long userId = data.getUserId();
        Long planId = data.getPlanId();
        
        log.info("【订阅到期任务】开始处理订阅到期 - subscriptionId: {}, userId: {}, planId: {}", 
                subscriptionId, userId, planId);

        try {
            // 调用服务处理单个订阅到期
            boolean processed = userSubscriptionService.processSpecificSubscriptionExpiration(subscriptionId);
            
            if (processed) {
                log.info("【订阅到期任务】订阅到期处理成功 - subscriptionId: {}", subscriptionId);
            } else {
                log.warn("【订阅到期任务】订阅可能已处理或不存在 - subscriptionId: {}", subscriptionId);
            }

        } catch (Exception e) {
            log.error("【订阅到期任务】处理订阅到期失败 - subscriptionId: {}, error: {}", 
                    subscriptionId, e.getMessage(), e);
            throw e; // 重新抛出异常，让 db-scheduler 处理重试
        }
    }

    /**
     * 订阅数据载荷
     */
    public static class SubscriptionData {
        private Long subscriptionId;
        private Long userId;
        private Long planId;
        private String planName;

        // 无参构造器（序列化需要）
        public SubscriptionData() {}

        public SubscriptionData(Long subscriptionId, Long userId, Long planId, String planName) {
            this.subscriptionId = subscriptionId;
            this.userId = userId;
            this.planId = planId;
            this.planName = planName;
        }

        // Getters and Setters
        public Long getSubscriptionId() { return subscriptionId; }
        public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
        
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
    }
}
