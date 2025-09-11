-- DB-Scheduler所需的表结构（PostgreSQL版本）
-- 创建时间：2024-01-02
-- 说明：db-scheduler需要这些表来存储定时任务的状态和执行记录

-- 1. 定时任务调度表（从V1.0.0迁移过来）
CREATE TABLE IF NOT EXISTS t_scheduled_task (
    task_name VARCHAR(40) NOT NULL,
    task_instance VARCHAR(40) NOT NULL,
    task_data BYTEA,
    execution_time TIMESTAMP(6) NOT NULL,
    picked BOOLEAN NOT NULL,
    picked_by VARCHAR(50),
    last_success TIMESTAMP(6),
    last_failure TIMESTAMP(6),
    consecutive_failures INTEGER,
    last_heartbeat TIMESTAMP(6),
    version BIGINT NOT NULL,
    PRIMARY KEY (task_name, task_instance)
);

-- 表注释
COMMENT ON TABLE t_scheduled_task IS '定时任务调度表';
COMMENT ON COLUMN t_scheduled_task.task_name IS '任务名称';
COMMENT ON COLUMN t_scheduled_task.task_instance IS '任务实例ID';
COMMENT ON COLUMN t_scheduled_task.task_data IS '任务数据';
COMMENT ON COLUMN t_scheduled_task.execution_time IS '执行时间';
COMMENT ON COLUMN t_scheduled_task.picked IS '是否被选中执行';
COMMENT ON COLUMN t_scheduled_task.picked_by IS '执行者标识';
COMMENT ON COLUMN t_scheduled_task.last_success IS '最后成功时间';
COMMENT ON COLUMN t_scheduled_task.last_failure IS '最后失败时间';
COMMENT ON COLUMN t_scheduled_task.consecutive_failures IS '连续失败次数';
COMMENT ON COLUMN t_scheduled_task.last_heartbeat IS '最后心跳时间';
COMMENT ON COLUMN t_scheduled_task.version IS '版本号（乐观锁）';

-- 索引
CREATE INDEX execution_time_idx ON t_scheduled_task(execution_time);
CREATE INDEX last_heartbeat_idx ON t_scheduled_task(last_heartbeat);

-- 2. 任务执行日志表（用于记录任务执行历史，非db-scheduler必需，但有助于监控）
CREATE TABLE IF NOT EXISTS t_schedule_execution_log (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,
    task_instance VARCHAR(200) NOT NULL,
    execution_time TIMESTAMP(6) NOT NULL,
    actual_execution_time TIMESTAMP(6),
    completion_time TIMESTAMP(6),
    success BOOLEAN,
    duration_ms BIGINT,
    result_message TEXT,
    error_message TEXT,
    executor_id VARCHAR(50),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_schedule_execution_log IS '定时任务执行日志表';
COMMENT ON COLUMN t_schedule_execution_log.id IS '主键ID';
COMMENT ON COLUMN t_schedule_execution_log.task_name IS '任务名称';
COMMENT ON COLUMN t_schedule_execution_log.task_instance IS '任务实例ID';
COMMENT ON COLUMN t_schedule_execution_log.execution_time IS '计划执行时间';
COMMENT ON COLUMN t_schedule_execution_log.actual_execution_time IS '实际执行时间';
COMMENT ON COLUMN t_schedule_execution_log.completion_time IS '完成时间';
COMMENT ON COLUMN t_schedule_execution_log.success IS '是否执行成功';
COMMENT ON COLUMN t_schedule_execution_log.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN t_schedule_execution_log.result_message IS '执行结果消息';
COMMENT ON COLUMN t_schedule_execution_log.error_message IS '错误信息';
COMMENT ON COLUMN t_schedule_execution_log.executor_id IS '执行者ID';
COMMENT ON COLUMN t_schedule_execution_log.create_time IS '创建时间';

CREATE INDEX idx_schedule_execution_log_task_name ON t_schedule_execution_log(task_name);
CREATE INDEX idx_schedule_execution_log_execution_time ON t_schedule_execution_log(execution_time);
CREATE INDEX idx_schedule_execution_log_create_time ON t_schedule_execution_log(create_time);
CREATE INDEX idx_schedule_execution_log_success ON t_schedule_execution_log(success);

-- 3. 积分发放记录表（用于记录每日积分发放的详细情况）
CREATE TABLE IF NOT EXISTS t_daily_credit_grant_log (
    id BIGSERIAL PRIMARY KEY,
    grant_date DATE NOT NULL,
    total_users INTEGER NOT NULL DEFAULT 0,
    success_users INTEGER NOT NULL DEFAULT 0,
    failed_users INTEGER NOT NULL DEFAULT 0,
    skipped_users INTEGER NOT NULL DEFAULT 0,
    total_credits DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    execution_duration_ms BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    result_message TEXT,
    created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_daily_credit_grant_log IS '每日积分发放日志表';
COMMENT ON COLUMN t_daily_credit_grant_log.id IS '主键ID';
COMMENT ON COLUMN t_daily_credit_grant_log.grant_date IS '发放日期';
COMMENT ON COLUMN t_daily_credit_grant_log.total_users IS '总用户数';
COMMENT ON COLUMN t_daily_credit_grant_log.success_users IS '成功发放用户数';
COMMENT ON COLUMN t_daily_credit_grant_log.failed_users IS '失败用户数';
COMMENT ON COLUMN t_daily_credit_grant_log.skipped_users IS '跳过用户数（已发放过）';
COMMENT ON COLUMN t_daily_credit_grant_log.total_credits IS '总发放积分';
COMMENT ON COLUMN t_daily_credit_grant_log.execution_duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN t_daily_credit_grant_log.start_time IS '开始时间';
COMMENT ON COLUMN t_daily_credit_grant_log.end_time IS '结束时间';
COMMENT ON COLUMN t_daily_credit_grant_log.result_message IS '执行结果详情';
COMMENT ON COLUMN t_daily_credit_grant_log.created_by IS '创建者';
COMMENT ON COLUMN t_daily_credit_grant_log.create_time IS '创建时间';

CREATE UNIQUE INDEX uk_daily_credit_grant_log_grant_date ON t_daily_credit_grant_log(grant_date);
CREATE INDEX idx_daily_credit_grant_log_create_time ON t_daily_credit_grant_log(create_time);
