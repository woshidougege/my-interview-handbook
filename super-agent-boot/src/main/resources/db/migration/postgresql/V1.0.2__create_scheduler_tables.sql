-- DB-Scheduler所需的表结构
-- 创建时间：2024-01-02
-- 说明：db-scheduler需要这些表来存储定时任务的状态和执行记录

-- 创建更新时间触发器函数（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. 定时对话任务表
CREATE TABLE IF NOT EXISTS t_scheduled_chat_task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    chat_task_id BIGINT,
    task_name VARCHAR(100) NOT NULL,
    cron_expression VARCHAR(50) NOT NULL,
    prompt TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    last_execution_time TIMESTAMP,
    next_execution_time TIMESTAMP,
    task_type SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    schedule_config TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_scheduled_chat_task IS '定时对话任务表';
COMMENT ON COLUMN t_scheduled_chat_task.id IS '主键ID';
COMMENT ON COLUMN t_scheduled_chat_task.user_id IS '用户ID';
COMMENT ON COLUMN t_scheduled_chat_task.workspace_id IS '工作空间ID';
COMMENT ON COLUMN t_scheduled_chat_task.chat_task_id IS '对话任务ID';
COMMENT ON COLUMN t_scheduled_chat_task.task_name IS '任务名称';
COMMENT ON COLUMN t_scheduled_chat_task.cron_expression IS 'Cron表达式';
COMMENT ON COLUMN t_scheduled_chat_task.prompt IS '对话提示词';
COMMENT ON COLUMN t_scheduled_chat_task.status IS '状态: 1启用 0禁用';
COMMENT ON COLUMN t_scheduled_chat_task.last_execution_time IS '上次执行时间';
COMMENT ON COLUMN t_scheduled_chat_task.next_execution_time IS '下次执行时间';
COMMENT ON COLUMN t_scheduled_chat_task.task_type IS '任务类型: 0-一次性任务 1-可重复任务';
COMMENT ON COLUMN t_scheduled_chat_task.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_scheduled_chat_task.schedule_config IS '任务调度配置（JSON格式存储）';
COMMENT ON COLUMN t_scheduled_chat_task.create_time IS '创建时间';
COMMENT ON COLUMN t_scheduled_chat_task.update_time IS '更新时间';
COMMENT ON COLUMN t_scheduled_chat_task.create_by IS '创建人ID';
COMMENT ON COLUMN t_scheduled_chat_task.update_by IS '更新人ID';

CREATE INDEX idx_scheduled_chat_task_user_id ON t_scheduled_chat_task(user_id);
CREATE INDEX idx_scheduled_chat_task_workspace_id ON t_scheduled_chat_task(workspace_id);
CREATE INDEX idx_scheduled_chat_task_chat_task_id ON t_scheduled_chat_task(chat_task_id);
CREATE INDEX idx_scheduled_chat_task_status ON t_scheduled_chat_task(status);
CREATE INDEX idx_scheduled_chat_task_next_execution_time ON t_scheduled_chat_task(next_execution_time);
CREATE INDEX idx_scheduled_chat_task_deleted ON t_scheduled_chat_task(deleted);
CREATE INDEX idx_scheduled_chat_task_user_deleted_status ON t_scheduled_chat_task(user_id, deleted, status);
CREATE INDEX idx_scheduled_chat_task_schedule_config ON t_scheduled_chat_task(schedule_config);

-- 为定时对话任务表创建更新时间触发器
CREATE TRIGGER update_t_scheduled_chat_task_updated_at BEFORE UPDATE ON t_scheduled_chat_task
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();