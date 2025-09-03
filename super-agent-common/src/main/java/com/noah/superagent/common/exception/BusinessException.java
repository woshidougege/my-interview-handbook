package com.noah.superagent.common.exception;

import com.noah.superagent.common.enums.ResponseCodeEnum;
import lombok.Getter;

/**
 * 业务异常类
 * 
 * @author System
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = ResponseCodeEnum.BUSINESS_ERROR.getCode();
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResponseCodeEnum responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }

    public BusinessException(ResponseCodeEnum responseCode, String customMessage) {
        super(customMessage);
        this.code = responseCode.getCode();
        this.message = customMessage;
    }

    /**
     * 快速抛出业务异常
     */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 快速抛出业务异常
     */
    public static void throwIf(boolean condition, ResponseCodeEnum responseCode) {
        if (condition) {
            throw new BusinessException(responseCode);
        }
    }
}
