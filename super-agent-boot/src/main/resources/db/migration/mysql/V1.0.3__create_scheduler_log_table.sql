-- DB-Scheduler Log 历史任务记录表（MySQL版本）
-- 创建时间：2025-09-12
-- 说明：db-scheduler-log 标准表结构，用于存储任务执行的详细历史记录

-- DB-Scheduler 历史任务记录表（db-scheduler-log标准表，以t开头）
CREATE TABLE IF NOT EXISTS `t_scheduled_execution_logs` (
    `id`                   BIGINT                   NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_name`            VARCHAR(40)              NOT NULL COMMENT '任务名称',
    `task_instance`        VARCHAR(40)              NOT NULL COMMENT '任务实例ID',
    `task_data`            BLOB COMMENT '任务数据',
    `picked_by`            VARCHAR(50) COMMENT '执行者标识',
    `time_started`         TIMESTAMP(6)             NOT NULL COMMENT '任务开始执行时间',
    `time_finished`        TIMESTAMP(6)             NOT NULL COMMENT '任务完成时间',
    `succeeded`            BOOLEAN                  NOT NULL COMMENT '任务是否执行成功',
    `duration_ms`          BIGINT                   NOT NULL COMMENT '任务执行耗时（毫秒）',
    `exception_class`      VARCHAR(1000) COMMENT '异常类名',
    `exception_message`    BLOB COMMENT '异常消息',
    `exception_stacktrace` BLOB COMMENT '异常堆栈信息',
    INDEX `idx_t_scheduled_execution_logs_started` (`time_started`),
    INDEX `idx_t_scheduled_execution_logs_task_name` (`task_name`),
    INDEX `idx_t_scheduled_execution_logs_exception_class` (`exception_class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='db-scheduler 任务执行历史记录表（标准格式）';
