package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.CreditDeductionTaskStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分扣减任务实体
 * 用于异步处理积分扣减，确保系统重启后任务不丢失
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_credit_deduction_task")
public class CreditDeductionTaskEntity extends BaseEntity {


    /**
     * 任务ID（唯一标识）
     */
    @Column(value = "task_id")
    private String taskId;

    /**
     * 用户ID
     */
    @Column(value = "user_id")
    private Long userId;

    /**
     * 扣减金额
     */
    @Column(value = "amount")
    private BigDecimal amount;

    /**
     * 扣减描述
     */
    @Column(value = "description")
    private String description;

    /**
     * 关联订单ID
     */
    @Column(value = "related_order_id")
    private Long relatedOrderId;

    /**
     * 关联的资源使用记录ID
     */
    @Column(value = "resource_usage_record_id")
    private Long resourceUsageRecordId;

    /**
     * 任务状态
     */
    @Column(value = "status")
    private CreditDeductionTaskStatusEnum status;

    /**
     * 重试次数
     */
    @Column(value = "retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @Column(value = "max_retry_count")
    private Integer maxRetryCount;

    /**
     * 错误信息
     */
    @Column(value = "error_message")
    private String errorMessage;

    /**
     * 最后错误时间
     */
    @Column(value = "last_error_time")
    private LocalDateTime lastErrorTime;

    /**
     * 计划执行时间
     */
    @Column(value = "scheduled_time")
    private LocalDateTime scheduledTime;

    /**
     * 实际执行时间
     */
    @Column(value = "executed_time")
    private LocalDateTime executedTime;

    /**
     * 完成时间
     */
    @Column(value = "completed_time")
    private LocalDateTime completedTime;
}
