package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资源使用记录表
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_resource_usage_record")
public class ResourceUsageRecordEntity extends BaseEntity {

    /**
     * 请求ID（幂等键）
     */
    @Column(value = "request_id")
    private String requestId;

    /**
     * 报告ID
     */
    @Column(value = "report_id")
    private String reportId;

    /**
     * 用户外部键
     */
    @Column(value = "user_id")
    private String userId;

    /**
     * 智能体ID
     */
    @Column(value = "agent_id")
    private String agentId;

    /**
     * 会话ID
     */
    @Column(value = "session_id")
    private String sessionId;

    /**
     * 任务类型
     */
    @Column(value = "task_type")
    private String taskType;

    /**
     * 任务描述
     */
    @Column(value = "task_description")
    private String taskDescription;

    /**
     * 资源类型
     * MODEL, FUNCTION, MEDIA
     */
    @Column(value = "resource_type")
    private String resourceType;

    /**
     * 具体资源
     * 模型名称、功能类型、媒体类型等
     */
    @Column(value = "resource_name")
    private String resourceName;

    /**
     * 资源子类型
     * TEXT_GENERATION, IMAGE_GENERATION, VIDEO_GENERATION等
     */
    @Column(value = "resource_subtype")
    private String resourceSubtype;

    /**
     * 使用量JSON
     * 存储具体的使用量数据（Token数、次数、秒数等）
     */
    @Column(value = "usage_data")
    private String usageData;

    /**
     * 计费单位
     */
    @Column(value = "billing_unit")
    private String billingUnit;

    /**
     * 使用量
     */
    @Column(value = "usage_amount")
    private BigDecimal usageAmount;

    /**
     * 单价
     */
    @Column(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 计费金额
     */
    @Column(value = "billing_amount")
    private BigDecimal billingAmount;

    /**
     * 请求描述
     */
    @Column(value = "description")
    private String description;
}
