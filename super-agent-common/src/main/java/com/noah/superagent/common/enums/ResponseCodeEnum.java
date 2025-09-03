package com.noah.superagent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author System
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {

    // ========== 成功 ==========
    SUCCESS(200, "操作成功"),
    
    // ========== 客户端错误 ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // ========== 业务错误 ==========
    BUSINESS_ERROR(1001, "业务处理失败"),
    PHONE_EXISTS(1002, "手机号已存在"),
    USER_NOT_FOUND(1003, "用户不存在"),
    INSUFFICIENT_CREDITS(1004, "积分余额不足"),
    PLAN_NOT_FOUND(1005, "套餐不存在"),
    SUBSCRIPTION_EXPIRED(1006, "订阅已过期"),
    
    // ========== 系统错误 ==========
    INTERNAL_ERROR(500, "系统内部错误"),
    DATABASE_ERROR(500, "数据库操作失败"),
    NETWORK_ERROR(500, "网络异常");

    private final Integer code;
    private final String message;
}
