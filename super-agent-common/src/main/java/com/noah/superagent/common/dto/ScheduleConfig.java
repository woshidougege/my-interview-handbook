package com.noah.superagent.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务调度配置
 * 用于前端友好的参数配置，替代复杂的Cron表达式
 * 支持重复任务的配置
 *
 * @author System
 * @since 1.0.0
 */
@Data
public class ScheduleConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 执行周期类型
     * ONE_TIME: 一次性任务
     * DAILY: 每天执行
     * WEEKLY: 每周执行
     * MONTHLY: 每月执行
     */
    private String cycleType;

    /**
     * 执行时间 (HH:mm:ss格式)
     * 示例: "14:30:00" 表示下午2点30分0秒
     */
    private String executionTime;

    /**
     * 执行日期 (yyyy-MM-dd格式，仅一次性任务使用)
     * 示例: "2024-12-25"
     */
    private String executionDate;

    /**
     * 星期几 (1-7，仅每周任务使用)
     * 1表示周一，7表示周日
     */
    private Integer dayOfWeek;

    /**
     * 日期 (1-31，仅每月任务使用)
     */
    private Integer dayOfMonth;
}