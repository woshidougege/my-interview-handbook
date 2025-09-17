-- 创建积分扣减任务表
-- 用于异步处理积分扣减，确保系统重启后任务不丢失

CREATE TABLE IF NOT EXISTS t_credit_deduction_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    related_order_id BIGINT,
    resource_usage_record_id BIGINT,
    
    -- 任务状态和重试信息
    status SMALLINT NOT NULL DEFAULT 0, -- 0-待处理，1-处理中，2-成功，3-失败
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry_count INTEGER NOT NULL DEFAULT 3,
    
    -- 错误信息
    error_message TEXT,
    last_error_time TIMESTAMP,
    
    -- 时间字段
    scheduled_time TIMESTAMP NOT NULL,
    executed_time TIMESTAMP,
    completed_time TIMESTAMP,
    
    -- 标准审计字段
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    version INTEGER DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_credit_deduction_task_user_id ON t_credit_deduction_task (user_id);
CREATE INDEX IF NOT EXISTS idx_credit_deduction_task_status ON t_credit_deduction_task (status);
CREATE INDEX IF NOT EXISTS idx_credit_deduction_task_scheduled_time ON t_credit_deduction_task (scheduled_time);
CREATE INDEX IF NOT EXISTS idx_credit_deduction_task_create_time ON t_credit_deduction_task (create_time);
CREATE INDEX IF NOT EXISTS idx_credit_deduction_task_resource_usage_record_id ON t_credit_deduction_task (resource_usage_record_id);

-- 添加表注释
COMMENT ON TABLE t_credit_deduction_task IS '积分扣减任务表';
COMMENT ON COLUMN t_credit_deduction_task.id IS '主键ID';
COMMENT ON COLUMN t_credit_deduction_task.task_id IS '任务ID（唯一标识）';
COMMENT ON COLUMN t_credit_deduction_task.user_id IS '用户ID';
COMMENT ON COLUMN t_credit_deduction_task.amount IS '扣减金额';
COMMENT ON COLUMN t_credit_deduction_task.description IS '扣减描述';
COMMENT ON COLUMN t_credit_deduction_task.related_order_id IS '关联订单ID';
COMMENT ON COLUMN t_credit_deduction_task.resource_usage_record_id IS '关联的资源使用记录ID';
COMMENT ON COLUMN t_credit_deduction_task.status IS '任务状态：0-待处理，1-处理中，2-成功，3-失败';
COMMENT ON COLUMN t_credit_deduction_task.retry_count IS '重试次数';
COMMENT ON COLUMN t_credit_deduction_task.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN t_credit_deduction_task.error_message IS '错误信息';
COMMENT ON COLUMN t_credit_deduction_task.last_error_time IS '最后错误时间';
COMMENT ON COLUMN t_credit_deduction_task.scheduled_time IS '计划执行时间';
COMMENT ON COLUMN t_credit_deduction_task.executed_time IS '实际执行时间';
COMMENT ON COLUMN t_credit_deduction_task.completed_time IS '完成时间';
COMMENT ON COLUMN t_credit_deduction_task.create_by IS '创建人ID';
COMMENT ON COLUMN t_credit_deduction_task.create_time IS '创建时间';
COMMENT ON COLUMN t_credit_deduction_task.update_by IS '更新人ID';
COMMENT ON COLUMN t_credit_deduction_task.update_time IS '更新时间';
COMMENT ON COLUMN t_credit_deduction_task.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_credit_deduction_task.version IS '版本号（乐观锁）';
