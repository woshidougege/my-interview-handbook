package com.noah.superagent.common.constants;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    
    /**
     * 等待支付
     */
    WAITING("waiting", "等待支付"),
    
    /**
     * 支付成功
     */
    PAID("paid", "支付成功"),
    
    /**
     * 支付失败
     */
    FAILED("failed", "支付失败"),
    
    /**
     * 支付超时/过期
     */
    EXPIRED("expired", "支付超时"),
    
    /**
     * 支付取消
     */
    CANCELLED("cancelled", "支付取消"),
    
    /**
     * 开始监听状态
     */
    LISTENING("listening", "开始监听支付状态"),
    
    /**
     * 待支付（旧状态，兼容性保留）
     */
    PENDING("pending", "等待支付"),
    
    /**
     * 退款处理中
     */
    REFUND_PROCESSING("refund_processing", "退款处理中"),
    
    /**
     * 退款成功
     */
    REFUND_SUCCESS("refund_success", "退款成功"),
    
    /**
     * 退款失败
     */
    REFUND_FAIL("refund_fail", "退款失败"),
    
    /**
     * 退款关闭
     */
    REFUND_CLOSED("refund_closed", "退款关闭"),
    
    /**
     * 退款异常
     */
    REFUND_ABNORMAL("refund_abnormal", "退款异常");
    
    private final String value;
    private final String description;
    
    PaymentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据值获取枚举
     */
    public static PaymentStatus fromValue(String value) {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 获取状态描述
     */
    public static String getStatusDescription(String value) {
        PaymentStatus status = fromValue(value);
        return status != null ? status.description : "未知状态";
    }
    
    /**
     * 判断是否为终态
     */
    public boolean isFinalStatus() {
        return this == PAID || this == FAILED || this == EXPIRED || this == CANCELLED 
            || this == REFUND_SUCCESS || this == REFUND_FAIL || this == REFUND_CLOSED || this == REFUND_ABNORMAL;
    }
    
    /**
     * 判断是否为进行中状态
     */
    public boolean isProcessingStatus() {
        return this == WAITING || this == PENDING || this == REFUND_PROCESSING;
    }
    
    /**
     * 判断是否为退款相关状态
     */
    public boolean isRefundStatus() {
        return this == REFUND_PROCESSING || this == REFUND_SUCCESS || this == REFUND_FAIL 
            || this == REFUND_CLOSED || this == REFUND_ABNORMAL;
    }
    
    /**
     * 静态方法：判断是否为退款相关状态
     */
    public static boolean isRefundStatus(String value) {
        PaymentStatus status = fromValue(value);
        return status != null && status.isRefundStatus();
    }
    
    /**
     * 静态方法：判断是否为终态
     */
    public static boolean isFinalStatus(String value) {
        PaymentStatus status = fromValue(value);
        return status != null && status.isFinalStatus();
    }
    
    /**
     * 静态方法：判断是否为进行中状态
     */
    public static boolean isProcessingStatus(String value) {
        PaymentStatus status = fromValue(value);
        return status != null && status.isProcessingStatus();
    }
    
    @Override
    public String toString() {
        return value;
    }
}
