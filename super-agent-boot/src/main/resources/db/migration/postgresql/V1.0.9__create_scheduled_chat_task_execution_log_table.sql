-- 定时对话任务执行日志表
-- 创建时间：2024-01-04
-- 说明：用于存储定时对话任务的执行历史记录

-- 1. 定时对话任务执行日志表
CREATE TABLE IF NOT EXISTS t_scheduled_chat_task_execution_log (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    chat_task_id BIGINT,
    task_name VARCHAR(100) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    execution_status SMALLINT NOT NULL,
    execution_result TEXT,
    duration BIGINT,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE t_scheduled_chat_task_execution_log IS '定时对话任务执行日志表';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.id IS '主键ID';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.task_id IS '定时任务ID';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.chat_task_id IS '对话任务ID';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.task_name IS '任务名称';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.start_time IS '执行开始时间';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.end_time IS '执行结束时间';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.execution_status IS '执行状态: 1成功 0失败';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.execution_result IS '执行结果';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.duration IS '执行耗时(毫秒)';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.error_message IS '错误信息';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.create_time IS '创建时间';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.update_time IS '更新时间';
COMMENT ON COLUMN t_scheduled_chat_task_execution_log.deleted IS '删除标记：0-未删除，1-已删除';

CREATE INDEX idx_scheduled_chat_task_execution_log_task_id ON t_scheduled_chat_task_execution_log(task_id);
CREATE INDEX idx_scheduled_chat_task_execution_log_chat_task_id ON t_scheduled_chat_task_execution_log(chat_task_id);
CREATE INDEX idx_scheduled_chat_task_execution_log_task_name ON t_scheduled_chat_task_execution_log(task_name);
CREATE INDEX idx_scheduled_chat_task_execution_log_start_time ON t_scheduled_chat_task_execution_log(start_time);
CREATE INDEX idx_scheduled_chat_task_execution_log_execution_status ON t_scheduled_chat_task_execution_log(execution_status);

-- 添加更新时间自动更新触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_t_scheduled_chat_task_execution_log_updated_at 
    BEFORE UPDATE ON t_scheduled_chat_task_execution_log 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();