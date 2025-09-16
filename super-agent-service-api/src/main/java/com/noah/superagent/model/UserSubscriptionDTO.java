package com.noah.superagent.model;

import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订阅DTO
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    name = "UserSubscriptionDTO",
    title = "用户订阅信息", 
    description = "用户订阅记录详细信息，包含订阅时间、价格、积分等数据"
)
public class UserSubscriptionDTO extends BaseDTO {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 套餐ID
     */
    @Schema(description = "套餐ID", example = "2")
    private Long planId;

    /**
     * 订阅开始时间
     */
    @Schema(description = "订阅开始时间", example = "2025-01-01T10:00:00")
    private LocalDateTime startTime;

    /**
     * 订阅结束时间
     */
    @Schema(description = "订阅结束时间", example = "2025-02-01T10:00:00")
    private LocalDateTime endTime;

    /**
     * 支付金额
     */
    @Schema(description = "支付金额", example = "39.9")
    private BigDecimal paidAmount;

    /**
     * 获得积分数量
     */
    @Schema(description = "获得积分数量", example = "5000")
    private BigDecimal creditAmount;

    /**
     * 订阅状态 ACTIVE-生效中 EXPIRED-已过期 CANCELLED-已取消
     */
    @Schema(description = "订阅状态", example = "ACTIVE", allowableValues = {"ACTIVE", "EXPIRED", "CANCELLED"})
    private SubscriptionStatusEnum status;

    /**
     * 支付订单号
     */
    @Schema(description = "支付订单号", example = "ORDER_2025010110001")
    private String payOrderNo;

    /**
     * 备注
     */
    @Schema(description = "备注信息", example = "基础版套餐订阅")
    private String remark;
}
