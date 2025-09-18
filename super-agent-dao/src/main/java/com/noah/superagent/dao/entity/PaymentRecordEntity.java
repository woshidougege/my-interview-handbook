package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_payment_record")
public class PaymentRecordEntity extends BaseEntity {


    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式 wechat/alipay
     */
    private String paymentMethod;

    /**
     * 第三方支付订单号
     */
    private String thirdPartyOrderNo;

    /**
     * 第三方交易流水号
     */
    private String thirdPartyTransactionNo;

    /**
     * 支付状态 pending/success/failed/cancelled
     */
    private String status;

    /**
     * 支付二维码
     */
    private String qrCode;

    /**
     * 支付链接
     */
    private String paymentUrl;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 第三方回调数据
     */
    private String callbackData;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 备注
     */
    private String remark;
}
