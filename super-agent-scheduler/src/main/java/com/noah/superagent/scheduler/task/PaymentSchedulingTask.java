package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.noah.superagent.common.event.PaymentOrderCreatedEvent;
import com.noah.superagent.common.event.PaymentSuccessEvent;
import com.noah.superagent.common.event.PaymentCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 支付调度事件监听器
 * <p>
 * 监听支付相关事件，精确安排超时检查任务
 * 采用事件驱动机制，避免资源浪费的轮询检查
 * <p>
 * 设计思路：
 * 1. 订单创建 → 安排30分钟后超时检查任务
 * 2. 支付成功 → 取消超时检查任务
 * 3. 用户取消订单 → 取消超时检查任务
 * 4. 支付失败 → 可安排重试任务（可选）
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSchedulingTask {

    private final Scheduler scheduler;
    private final PaymentTimeoutCheckTask paymentTimeoutCheckTask;

    /**
     * 监听订单创建事件，安排超时检查任务
     */
    @EventListener
    public void handleOrderCreated(PaymentOrderCreatedEvent event) {
        log.info("【支付调度】收到订单创建事件 - orderNo: {}, expiredAt: {}", 
                event.getOrderNo(), event.getExpiredAt());

        try {
            scheduleTimeoutCheckTask(
                event.getOrderNo(),
                event.getUserId(),
                event.getAmount(),
                event.getExpiredAt()
            );

            log.info("【支付调度】成功安排超时检查任务 - orderNo: {}", event.getOrderNo());

        } catch (Exception e) {
            log.error("【支付调度】安排超时检查任务失败 - orderNo: {}, error: {}", 
                    event.getOrderNo(), e.getMessage(), e);
        }
    }

    /**
     * 监听支付成功事件，取消超时检查任务
     */
    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("【支付调度】收到支付成功事件 - orderNo: {}", event.getOrderNo());

        try {
            cancelTimeoutCheckTask(event.getOrderNo());
            log.info("【支付调度】成功取消超时检查任务 - orderNo: {}", event.getOrderNo());

        } catch (Exception e) {
            log.error("【支付调度】取消超时检查任务失败 - orderNo: {}, error: {}", 
                    event.getOrderNo(), e.getMessage(), e);
            // 取消失败不影响主流程
        }
    }

    /**
     * 监听订单取消事件，取消超时检查任务
     */
    @EventListener
    public void handleOrderCancelled(PaymentCancelledEvent event) {
        log.info("【支付调度】收到订单取消事件 - orderNo: {}, cancelReason: {}", 
                event.getOrderNo(), event.getCancelReason());

        try {
            cancelTimeoutCheckTask(event.getOrderNo());
            log.info("【支付调度】成功取消超时检查任务 - orderNo: {}", event.getOrderNo());

        } catch (Exception e) {
            log.error("【支付调度】取消超时检查任务失败 - orderNo: {}, error: {}", 
                    event.getOrderNo(), e.getMessage(), e);
            // 取消失败不影响主流程
        }
    }

    /**
     * 安排支付超时检查任务
     */
    private void scheduleTimeoutCheckTask(String orderNo, Long userId, 
                                        java.math.BigDecimal amount, LocalDateTime expiredAt) {
        try {
            // 创建任务数据
            PaymentTimeoutCheckTask.PaymentTimeoutData data = 
                new PaymentTimeoutCheckTask.PaymentTimeoutData(orderNo, userId, amount);

            // 转换为Instant
            Instant executeAt = expiredAt.atZone(ZoneId.systemDefault()).toInstant();

            // 安排任务
            String taskId = "payment-timeout-" + orderNo;
            scheduler.schedule(
                paymentTimeoutCheckTask.getTask().instance(taskId, data),
                executeAt
            );

            log.info("已安排支付超时检查任务 - orderNo: {}, executeAt: {}", orderNo, executeAt);

        } catch (Exception e) {
            log.error("安排支付超时检查任务失败 - orderNo: {}, error: {}", orderNo, e.getMessage(), e);
            throw new RuntimeException("安排支付超时检查任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消支付超时检查任务
     */
    private void cancelTimeoutCheckTask(String orderNo) {
        try {
            String taskId = "payment-timeout-" + orderNo;
            PaymentTimeoutCheckTask.PaymentTimeoutData dummyData = 
                new PaymentTimeoutCheckTask.PaymentTimeoutData(orderNo, null, null);

            scheduler.cancel(
                paymentTimeoutCheckTask.getTask().instance(taskId, dummyData)
            );

            log.info("支付超时检查任务取消请求已发送 - orderNo: {}", orderNo);

        } catch (Exception e) {
            log.error("取消支付超时检查任务失败 - orderNo: {}, error: {}", orderNo, e.getMessage(), e);
            // 取消失败不抛异常，避免影响主流程
        }
    }
}
