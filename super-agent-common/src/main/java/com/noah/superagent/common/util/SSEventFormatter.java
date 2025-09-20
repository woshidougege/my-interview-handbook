package com.noah.superagent.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SSE事件格式化工具类
 */
public class SSEventFormatter {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 格式化SSE事件
     *
     * @param eventType 事件类型
     * @param data      事件数据
     * @return 格式化后的SSE事件字符串
     */
    public static String formatEvent(String eventType, String data) {
        StringBuilder sb = new StringBuilder();
        sb.append("id: ").append(System.currentTimeMillis()).append("\n");
        sb.append("event: ").append(eventType).append("\n");
        sb.append("data: ").append(data).append("\n");
        sb.append("retry: 10000\n"); // 10秒后重试
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 格式化带时间戳的SSE事件
     *
     * @param eventType 事件类型
     * @param data      事件数据
     * @return 格式化后的SSE事件字符串
     */
    public static String formatEventWithTimestamp(String eventType, String data) {
        StringBuilder sb = new StringBuilder();
        sb.append("id: ").append(System.currentTimeMillis()).append("\n");
        sb.append("event: ").append(eventType).append("\n");
        sb.append("data: ").append(data).append("\n");
        sb.append("timestamp: ").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 格式化错误事件
     *
     * @param errorMessage 错误消息
     * @return 格式化后的SSE错误事件字符串
     */
    public static String formatErrorEvent(String errorMessage) {
        return formatEvent("error", errorMessage);
    }
    
    /**
     * 格式化结束事件
     *
     * @return 格式化后的SSE结束事件字符串
     */
    public static String formatEndEvent() {
        return formatEvent("end", "end");
    }
}