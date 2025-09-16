package com.noah.superagent.common.util;

import com.noah.superagent.common.dto.ScheduleConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Cron表达式工具类
 * 用于根据前端友好的参数生成cron表达式
 */
public class CronExpressionUtil {

    private static final Set<String> VALID_CYCLE_TYPES = new HashSet<>(
            Arrays.asList("ONE_TIME", "DAILY", "WEEKLY", "MONTHLY")
    );
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 根据调度配置生成cron表达式
     *
     * @param scheduleConfig 调度配置
     * @return cron表达式
     * @throws IllegalArgumentException 当配置参数不合法时抛出异常
     */
    public static String generateCronExpression(ScheduleConfig scheduleConfig) {
        if (scheduleConfig == null) {
            throw new IllegalArgumentException("调度配置不能为空");
        }

        validateScheduleConfig(scheduleConfig);

        String executionTime = scheduleConfig.getExecutionTime();
        LocalTime time = LocalTime.parse(executionTime);

        String second = String.valueOf(time.getSecond());
        String minute = String.valueOf(time.getMinute());
        String hour = String.valueOf(time.getHour());

        switch (scheduleConfig.getCycleType().toUpperCase()) {
            case "ONE_TIME":
                // 一次性任务，指定具体日期执行
                String executionDate = scheduleConfig.getExecutionDate();
                // 如果没有executionDate字段或者executionDate时间在当前时间之前，就默认为最近的下一次cron时间
                if (executionDate == null || executionDate.isEmpty() || 
                    LocalDate.parse(executionDate, DATE_FORMATTER).isBefore(LocalDate.now())) {
                    // 使用明天作为默认日期
                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                    int day = tomorrow.getDayOfMonth();
                    int month = tomorrow.getMonthValue();
                    return String.format("%s %s %s %d %d ?", second, minute, hour, day, month);
                } else {
                    LocalDate date = LocalDate.parse(executionDate, DATE_FORMATTER);
                    int day = date.getDayOfMonth();
                    int month = date.getMonthValue();
                    return String.format("%s %s %s %d %d ?", second, minute, hour, day, month);
                }
            case "DAILY":
                // 每天执行
                return String.format("%s %s %s * * ?", second, minute, hour);
            case "WEEKLY":
                // 每周执行
                Integer dayOfWeek = scheduleConfig.getDayOfWeek();
                return String.format("%s %s %s ? * %d", second, minute, hour, dayOfWeek);
            case "MONTHLY":
                // 每月执行
                Integer dayOfMonth = scheduleConfig.getDayOfMonth();
                return String.format("%s %s %s %d * ?", second, minute, hour, dayOfMonth);
            default:
                throw new IllegalArgumentException("不支持的执行周期类型: " + scheduleConfig.getCycleType());
        }
    }

    /**
     * 验证调度配置参数
     *
     * @param scheduleConfig 调度配置
     * @throws IllegalArgumentException 当配置参数不合法时抛出异常
     */
    private static void validateScheduleConfig(ScheduleConfig scheduleConfig) {
        if (scheduleConfig.getCycleType() == null || scheduleConfig.getCycleType().isEmpty()) {
            throw new IllegalArgumentException("执行周期类型不能为空");
        }

        if (!VALID_CYCLE_TYPES.contains(scheduleConfig.getCycleType().toUpperCase())) {
            throw new IllegalArgumentException("不支持的执行周期类型: " + scheduleConfig.getCycleType());
        }

        if (scheduleConfig.getExecutionTime() == null || scheduleConfig.getExecutionTime().isEmpty()) {
            throw new IllegalArgumentException("执行时间不能为空");
        }

        try {
            LocalTime.parse(scheduleConfig.getExecutionTime(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("执行时间格式不正确，应为HH:mm:ss格式");
        }

        if ("ONE_TIME".equalsIgnoreCase(scheduleConfig.getCycleType())) {
            // 一次性任务不再强制要求executionDate，如果没有或者已过期则使用默认值
            if (scheduleConfig.getExecutionDate() != null && !scheduleConfig.getExecutionDate().isEmpty()) {
                try {
                    LocalDate.parse(scheduleConfig.getExecutionDate(), DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("执行日期格式不正确，应为yyyy-MM-dd格式");
                }
            }
        }

        if ("WEEKLY".equalsIgnoreCase(scheduleConfig.getCycleType())) {
            if (scheduleConfig.getDayOfWeek() == null) {
                throw new IllegalArgumentException("每周执行必须指定星期几(1-7)");
            }
            if (scheduleConfig.getDayOfWeek() < 1 || scheduleConfig.getDayOfWeek() > 7) {
                throw new IllegalArgumentException("星期几必须在1-7范围内");
            }
        }

        if ("MONTHLY".equalsIgnoreCase(scheduleConfig.getCycleType())) {
            if (scheduleConfig.getDayOfMonth() == null) {
                throw new IllegalArgumentException("每月执行必须指定日期(1-31)");
            }
            if (scheduleConfig.getDayOfMonth() < 1 || scheduleConfig.getDayOfMonth() > 31) {
                throw new IllegalArgumentException("日期必须在1-31范围内");
            }
        }
    }

    /**
     * 将cron表达式转换为调度配置参数（用于回显）
     *
     * @param cronExpression cron表达式
     * @return 调度配置参数，当无法解析时返回null
     */
    public static ScheduleConfig parseCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isEmpty()) {
            return null;
        }

        String[] parts = cronExpression.split(" ");
        if (parts.length < 6) {
            return null; // 无法解析的cron表达式
        }
        
        ScheduleConfig config = new ScheduleConfig();

        // 解析时间 (秒 分 时)
        String second = parts[0];
        String minute = parts[1];
        String hour = parts[2];

        config.setExecutionTime(String.format("%02d:%02d:%02d", 
                parseTimeValue(hour), 
                parseTimeValue(minute), 
                parseTimeValue(second)));

        // 根据日和星期的值判断周期类型
        String dayOfMonth = parts[3];
        String month = parts[4];
        String dayOfWeek = parts[5];

        if ("?".equals(dayOfMonth) && !"?".equals(dayOfWeek)) {
            // 每周执行
            config.setCycleType("WEEKLY");
            config.setDayOfWeek(parseTimeValue(dayOfWeek));
        } else if (!"?".equals(dayOfMonth) && !"*".equals(month)) {
            // 一次性任务 (指定具体日期)
            config.setCycleType("ONE_TIME");
            // 注意：这里无法准确还原原始日期，因为我们不知道年份
            config.setExecutionDate(String.format("2024-%02d-%02d", 
                    parseTimeValue(month), parseTimeValue(dayOfMonth)));
        } else if ("*".equals(dayOfMonth) && "?".equals(dayOfWeek)) {
            // 每天执行
            config.setCycleType("DAILY");
        } else {
            // 默认设为每天执行
            config.setCycleType("DAILY");
        }

        return config;
    }

    /**
     * 解析时间值
     *
     * @param value 时间值字符串
     * @return 解析后的整数值
     */
    private static int parseTimeValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 如果不是数字，返回默认值0
            return 0;
        }
    }
}