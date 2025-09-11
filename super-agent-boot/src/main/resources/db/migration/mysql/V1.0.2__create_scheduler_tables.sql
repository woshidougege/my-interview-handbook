-- DB-Scheduler所需的表结构
-- 创建时间：2024-01-02
-- 说明：db-scheduler需要这些表来存储定时任务的状态和执行记录

-- 1. 定时任务调度表（从V1.0.0迁移过来）
CREATE TABLE IF NOT EXISTS `t_scheduled_task` (
    `task_name` VARCHAR(40) NOT NULL COMMENT '任务名称',
    `task_instance` VARCHAR(40) NOT NULL COMMENT '任务实例ID',
    `task_data` BLOB COMMENT '任务数据',
    `execution_time` TIMESTAMP(6) NOT NULL COMMENT '执行时间',
    `picked` TINYINT(1) NOT NULL COMMENT '是否被选中执行',
    `picked_by` VARCHAR(50) COMMENT '执行者标识',
    `last_success` TIMESTAMP(6) NULL COMMENT '最后成功时间',
    `last_failure` TIMESTAMP(6) NULL COMMENT '最后失败时间',
    `consecutive_failures` INT COMMENT '连续失败次数',
    `last_heartbeat` TIMESTAMP(6) NULL COMMENT '最后心跳时间',
    `version` BIGINT NOT NULL COMMENT '版本号（乐观锁）',
    PRIMARY KEY (`task_name`, `task_instance`),
    INDEX `execution_time_idx` (`execution_time`),
    INDEX `last_heartbeat_idx` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务调度表';

-- 2. 任务执行日志表（用于记录任务执行历史，非db-scheduler必需，但有助于监控）
CREATE TABLE IF NOT EXISTS `t_schedule_execution_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
    `task_instance` VARCHAR(200) NOT NULL COMMENT '任务实例ID',
    `execution_time` TIMESTAMP(6) NOT NULL COMMENT '计划执行时间',
    `actual_execution_time` TIMESTAMP(6) COMMENT '实际执行时间',
    `completion_time` TIMESTAMP(6) COMMENT '完成时间',
    `success` BOOLEAN COMMENT '是否执行成功',
    `duration_ms` BIGINT COMMENT '执行耗时（毫秒）',
    `result_message` TEXT COMMENT '执行结果消息',
    `error_message` TEXT COMMENT '错误信息',
    `executor_id` VARCHAR(50) COMMENT '执行者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_name` (`task_name`),
    INDEX `idx_execution_time` (`execution_time`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_success` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

-- 3. 积分发放记录表（用于记录每日积分发放的详细情况）
CREATE TABLE IF NOT EXISTS `t_daily_credit_grant_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `grant_date` DATE NOT NULL COMMENT '发放日期',
    `total_users` INT NOT NULL DEFAULT 0 COMMENT '总用户数',
    `success_users` INT NOT NULL DEFAULT 0 COMMENT '成功发放用户数',
    `failed_users` INT NOT NULL DEFAULT 0 COMMENT '失败用户数',
    `skipped_users` INT NOT NULL DEFAULT 0 COMMENT '跳过用户数（已发放过）',
    `total_credits` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '总发放积分',
    `execution_duration_ms` BIGINT COMMENT '执行耗时（毫秒）',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `result_message` TEXT COMMENT '执行结果详情',
    `created_by` VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grant_date` (`grant_date`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日积分发放日志表';
