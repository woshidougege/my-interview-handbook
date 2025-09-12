-- DB-Scheduler Log 历史任务记录表（PostgreSQL版本）
-- 创建时间：2025-09-12
-- 说明：db-scheduler-log 标准表结构，用于存储任务执行的详细历史记录

-- DB-Scheduler 历史任务记录表（db-scheduler-log标准表，以t开头）
CREATE TABLE IF NOT EXISTS t_scheduled_execution_logs (
    id                   BIGSERIAL                NOT NULL PRIMARY KEY,
    task_name            TEXT                     NOT NULL,
    task_instance        TEXT                     NOT NULL,
    task_data            BYTEA,
    picked_by            TEXT,
    time_started         TIMESTAMP WITH TIME ZONE NOT NULL,
    time_finished        TIMESTAMP WITH TIME ZONE NOT NULL,
    succeeded            BOOLEAN                  NOT NULL,
    duration_ms          BIGINT                   NOT NULL,
    exception_class      TEXT,
    exception_message    TEXT,
    exception_stacktrace TEXT
);

-- 表注释
COMMENT ON TABLE t_scheduled_execution_logs IS 'db-scheduler 任务执行历史记录表（标准格式）';
COMMENT ON COLUMN t_scheduled_execution_logs.id IS '主键ID';
COMMENT ON COLUMN t_scheduled_execution_logs.task_name IS '任务名称';
COMMENT ON COLUMN t_scheduled_execution_logs.task_instance IS '任务实例ID';
COMMENT ON COLUMN t_scheduled_execution_logs.task_data IS '任务数据';
COMMENT ON COLUMN t_scheduled_execution_logs.picked_by IS '执行者标识';
COMMENT ON COLUMN t_scheduled_execution_logs.time_started IS '任务开始执行时间';
COMMENT ON COLUMN t_scheduled_execution_logs.time_finished IS '任务完成时间';
COMMENT ON COLUMN t_scheduled_execution_logs.succeeded IS '任务是否执行成功';
COMMENT ON COLUMN t_scheduled_execution_logs.duration_ms IS '任务执行耗时（毫秒）';
COMMENT ON COLUMN t_scheduled_execution_logs.exception_class IS '异常类名';
COMMENT ON COLUMN t_scheduled_execution_logs.exception_message IS '异常消息';
COMMENT ON COLUMN t_scheduled_execution_logs.exception_stacktrace IS '异常堆栈信息';

-- 索引
CREATE INDEX idx_t_scheduled_execution_logs_started ON t_scheduled_execution_logs (time_started);
CREATE INDEX idx_t_scheduled_execution_logs_task_name ON t_scheduled_execution_logs (task_name);
CREATE INDEX idx_t_scheduled_execution_logs_exception_class ON t_scheduled_execution_logs (exception_class);
