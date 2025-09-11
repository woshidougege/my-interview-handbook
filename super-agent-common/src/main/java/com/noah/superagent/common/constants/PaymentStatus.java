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
    PENDING("pending", "等待支付");
    
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
        return this == PAID || this == FAILED || this == EXPIRED || this == CANCELLED;
    }
    
    /**
     * 判断是否为进行中状态
     */
    public boolean isProcessingStatus() {
        return this == WAITING || this == PENDING;
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
