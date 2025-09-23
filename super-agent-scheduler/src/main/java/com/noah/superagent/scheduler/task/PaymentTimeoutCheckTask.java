package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.noah.superagent.service.PaymentService;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 支付超时检查任务
 * <p>
 * 为每个订单创建精确的超时检查任务，替代频繁的轮询机制
 * 在订单创建时安排30分钟后的超时检查，支付成功后取消任务
 * <p>
 * 设计思路：
 * 1. 订单创建 → 安排30分钟后检查任务
 * 2. 支付成功 → 取消检查任务  
 * 3. 30分钟后 → 主动查询微信支付状态
 * 4. 系统重启 → db-scheduler自动恢复未执行的任务
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class PaymentTimeoutCheckTask {

    public static final String TASK_NAME = "payment-timeout-check";
    
    private final PaymentService paymentService;
    
    @Getter
    private final OneTimeTask<PaymentTimeoutData> task;

    public PaymentTimeoutCheckTask(PaymentService paymentService) {
        this.paymentService = paymentService;
        
        // 创建一次性任务，用于检查订单超时
        this.task = Tasks.oneTime(TASK_NAME, PaymentTimeoutData.class)
                .execute(this::checkPaymentTimeout);
    }

    /**
     * 检查单个订单是否超时
     */
    private void checkPaymentTimeout(TaskInstance<PaymentTimeoutData> taskInstance, 
                                   ExecutionContext executionContext) {
        PaymentTimeoutData data = taskInstance.getData();
        String orderNo = data.getOrderNo();
        
        log.info("【支付超时检查】开始检查订单 - orderNo: {}", orderNo);

        try {
            // 调用服务检查并处理订单超时
            boolean processed = paymentService.checkAndProcessPaymentTimeout(orderNo);
            
            if (processed) {
                log.info("【支付超时检查】订单状态已更新 - orderNo: {}", orderNo);
            } else {
                log.info("【支付超时检查】订单状态正常，无需处理 - orderNo: {}", orderNo);
            }

        } catch (Exception e) {
            log.error("【支付超时检查】处理订单超时失败 - orderNo: {}, error: {}", 
                    orderNo, e.getMessage(), e);
            throw e; // 重新抛出异常，让 db-scheduler 处理重试
        }
    }

    /**
     * 支付超时数据载荷
     */
    @Data
    public static class PaymentTimeoutData implements Serializable {
        // Getters and Setters
        private String orderNo;
        private Long userId;
        private java.math.BigDecimal amount;

        public PaymentTimeoutData(String orderNo, Long userId, java.math.BigDecimal amount) {
            this.orderNo = orderNo;
            this.userId = userId;
            this.amount = amount;
        }

    }
}
