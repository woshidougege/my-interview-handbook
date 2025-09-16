-- DB-Scheduler所需的表结构
-- 创建时间：2024-01-02
-- 说明：db-scheduler需要这些表来存储定时任务的状态和执行记录

-- 1. 定时对话任务表
CREATE TABLE IF NOT EXISTS `t_scheduled_chat_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
    `chat_task_id` BIGINT COMMENT '对话任务ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `cron_expression` VARCHAR(50) NOT NULL COMMENT 'Cron表达式',
    `prompt` TEXT NOT NULL COMMENT '对话提示词',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `last_execution_time` DATETIME COMMENT '上次执行时间',
    `next_execution_time` DATETIME COMMENT '下次执行时间',
    `task_type` TINYINT NOT NULL DEFAULT 1 COMMENT '任务类型: 0-一次性任务 1-可重复任务',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `schedule_config` TEXT COMMENT '任务调度配置（JSON格式存储）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_scheduled_chat_task_user_id` (`user_id`),
    INDEX `idx_scheduled_chat_task_workspace_id` (`workspace_id`),
    INDEX `idx_scheduled_chat_task_chat_task_id` (`chat_task_id`),
    INDEX `idx_scheduled_chat_task_status` (`status`),
    INDEX `idx_scheduled_chat_task_next_execution_time` (`next_execution_time`),
    INDEX `idx_scheduled_chat_task_deleted` (`deleted`),
    INDEX `idx_scheduled_chat_task_user_deleted_status` (`user_id`, `deleted`, `status`),
    INDEX `idx_scheduled_chat_task_schedule_config` (`schedule_config`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时对话任务表';

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

-- 3. 定时任务调度表（从V1.0.0迁移过来）
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
