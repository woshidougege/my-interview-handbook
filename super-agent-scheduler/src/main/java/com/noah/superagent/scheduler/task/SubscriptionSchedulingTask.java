package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.noah.superagent.common.event.SubscriptionActivatedEvent;
import com.noah.superagent.common.event.SubscriptionExtendedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * 订阅调度事件监听器
 * <p>
 * 监听订阅激活和延期事件，精确安排到期任务
 * 避免资源浪费的轮询检查方式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionSchedulingTask {

    private final Scheduler scheduler;
    private final SubscriptionExpirationTask subscriptionExpirationTask;

    /**
     * 监听订阅激活事件，安排到期任务
     */
    @EventListener
    public void handleSubscriptionActivated(SubscriptionActivatedEvent event) {
        log.info("【订阅调度】收到订阅激活事件 - subscriptionId: {}, userId: {}, endTime: {}", 
                event.getSubscriptionId(), event.getUserId(), event.getEndTime());

        try {
            scheduleExpirationTask(
                event.getSubscriptionId(),
                event.getUserId(),
                event.getPlanId(),
                event.getPlanName(),
                event.getEndTime()
            );
            
            log.info("【订阅调度】成功安排到期任务 - subscriptionId: {}", event.getSubscriptionId());

        } catch (Exception e) {
            log.error("【订阅调度】安排到期任务失败 - subscriptionId: {}, error: {}", 
                    event.getSubscriptionId(), e.getMessage(), e);
        }
    }

    /**
     * 监听订阅延期事件，重新安排到期任务
     */
    @EventListener
    public void handleSubscriptionExtended(SubscriptionExtendedEvent event) {
        log.info("【订阅调度】收到订阅延期事件 - subscriptionId: {}, newEndTime: {}", 
                event.getSubscriptionId(), event.getNewEndTime());

        try {
            // 先取消原任务
            cancelExpirationTask(event.getSubscriptionId());
            
            // 安排新任务
            scheduleExpirationTask(
                event.getSubscriptionId(),
                event.getUserId(),
                event.getPlanId(),
                event.getPlanName(),
                event.getNewEndTime()
            );
            
            log.info("【订阅调度】成功重新安排到期任务 - subscriptionId: {}", event.getSubscriptionId());

        } catch (Exception e) {
            log.error("【订阅调度】重新安排到期任务失败 - subscriptionId: {}, error: {}", 
                    event.getSubscriptionId(), e.getMessage(), e);
        }
    }

    /**
     * 安排订阅到期任务
     */
    private void scheduleExpirationTask(Long subscriptionId, Long userId, Long planId, 
                                      String planName, java.time.LocalDateTime endTime) {
        try {
            // 创建任务数据
            SubscriptionExpirationTask.SubscriptionData data = 
                new SubscriptionExpirationTask.SubscriptionData(subscriptionId, userId, planId, planName);

            // 转换为Instant
            Instant executeAt = endTime.atZone(ZoneId.systemDefault()).toInstant();
            
            // 使用正确的API安排任务
            String taskId = "subscription-expiration-" + subscriptionId;
            scheduler.schedule(
                subscriptionExpirationTask.getTask().instance(taskId, data),
                executeAt
            );

            log.info("已安排订阅到期任务 - subscriptionId: {}, executeAt: {}", subscriptionId, executeAt);

        } catch (Exception e) {
            log.error("安排订阅到期任务失败 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage(), e);
            throw new RuntimeException("安排订阅到期任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消订阅到期任务
     */
    private void cancelExpirationTask(Long subscriptionId) {
        try {
            String taskId = "subscription-expiration-" + subscriptionId;
            SubscriptionExpirationTask.SubscriptionData dummyData = 
                new SubscriptionExpirationTask.SubscriptionData(subscriptionId, null, null, null);
            
            scheduler.cancel(
                subscriptionExpirationTask.getTask().instance(taskId, dummyData)
            );

            log.info("订阅到期任务取消请求已发送 - subscriptionId: {}", subscriptionId);

        } catch (Exception e) {
            log.error("取消订阅到期任务失败 - subscriptionId: {}, error: {}", subscriptionId, e.getMessage(), e);
            // 取消失败不抛异常，避免影响主流程
        }
    }
}
