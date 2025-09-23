package com.noah.superagent.common.util;

import org.springframework.util.StringUtils;

/**
 * 安全工具类
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
public class SecurityUtils {
    
    /**
     * 对字符串进行HTML转义，防止XSS攻击
     * 
     * @param input 输入字符串
     * @return 转义后的安全字符串
     */
    public static String escapeHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
    
    /**
     * 生成安全的错误信息，不包含用户可控制的内容
     * 
     * @param errorType 错误类型
     * @return 安全的错误信息
     */
    public static String getSafeErrorMessage(String errorType) {
        switch (errorType) {
            case "STREAM_READ_ERROR":
                return "数据读取异常，请稍后重试";
            case "STREAM_WRITE_ERROR":
                return "数据传输异常，请稍后重试";
            case "CONNECTION_ERROR":
                return "连接异常，请检查网络后重试";
            case "TIMEOUT_ERROR":
                return "请求超时，请稍后重试";
            default:
                return "系统异常，请稍后重试";
        }
    }
    
    /**
     * 对错误信息进行安全处理
     * 只保留安全的错误类型信息，不输出具体的异常详情
     * 
     * @param exception 异常对象
     * @return 安全的错误信息
     */
    public static String sanitizeErrorMessage(Exception exception) {
        if (exception == null) {
            return getSafeErrorMessage("UNKNOWN");
        }
        
        String className = exception.getClass().getSimpleName();
        
        // 根据异常类型返回安全的错误信息
        if (className.contains("IOException")) {
            return getSafeErrorMessage("CONNECTION_ERROR");
        } else if (className.contains("TimeoutException")) {
            return getSafeErrorMessage("TIMEOUT_ERROR");
        } else if (className.contains("StreamException")) {
            return getSafeErrorMessage("STREAM_READ_ERROR");
        } else {
            return getSafeErrorMessage("UNKNOWN");
        }
    }
}
