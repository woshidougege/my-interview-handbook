-- 资源使用记录表（PostgreSQL版本）
CREATE TABLE t_resource_usage_record (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL,
    report_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    context_id VARCHAR(100),
    
    task_type VARCHAR(50) NOT NULL,
    task_description VARCHAR(500),
    
    resource_type VARCHAR(20) NOT NULL,
    resource_name VARCHAR(100) NOT NULL,
    resource_subtype VARCHAR(50),
    
    usage_data TEXT,
    billing_unit VARCHAR(20),
    usage_amount DECIMAL(15,6) DEFAULT 0,
    unit_price DECIMAL(10,6) DEFAULT 0,
    billing_amount DECIMAL(15,6) DEFAULT 0,
    
    description VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 表注释
COMMENT ON TABLE t_resource_usage_record IS '资源使用记录表';
COMMENT ON COLUMN t_resource_usage_record.id IS '主键ID';
COMMENT ON COLUMN t_resource_usage_record.request_id IS '请求ID（幂等键）';
COMMENT ON COLUMN t_resource_usage_record.report_id IS '报告ID';
COMMENT ON COLUMN t_resource_usage_record.user_id IS '用户ID（雪花算法生成）';
COMMENT ON COLUMN t_resource_usage_record.agent_id IS '智能体ID';
COMMENT ON COLUMN t_resource_usage_record.context_id IS '会话ID';
COMMENT ON COLUMN t_resource_usage_record.task_type IS '任务类型：INDUSTRY_RESEARCH_REPORT, PPT_GENERATION等';
COMMENT ON COLUMN t_resource_usage_record.task_description IS '任务描述';
COMMENT ON COLUMN t_resource_usage_record.resource_type IS '资源类型：MODEL, FUNCTION, MEDIA';
COMMENT ON COLUMN t_resource_usage_record.resource_name IS '资源名称：模型名称、功能类型、媒体类型等';
COMMENT ON COLUMN t_resource_usage_record.resource_subtype IS '资源子类型：TEXT_GENERATION, IMAGE_GENERATION等';
COMMENT ON COLUMN t_resource_usage_record.usage_data IS '使用量JSON数据（Token数、次数、秒数等）';
COMMENT ON COLUMN t_resource_usage_record.billing_unit IS '计费单位：TOKEN, TIMES, PAGES, COUNT, SECONDS';
COMMENT ON COLUMN t_resource_usage_record.usage_amount IS '使用量';
COMMENT ON COLUMN t_resource_usage_record.unit_price IS '单价（元）';
COMMENT ON COLUMN t_resource_usage_record.billing_amount IS '计费金额（元）';
COMMENT ON COLUMN t_resource_usage_record.description IS '描述';
COMMENT ON COLUMN t_resource_usage_record.create_time IS '创建时间';
COMMENT ON COLUMN t_resource_usage_record.update_time IS '更新时间';
COMMENT ON COLUMN t_resource_usage_record.create_by IS '创建人ID';
COMMENT ON COLUMN t_resource_usage_record.update_by IS '更新人ID';
COMMENT ON COLUMN t_resource_usage_record.deleted IS '删除标记：0-未删除，1-已删除';

-- 创建索引
CREATE INDEX idx_resource_usage_request_id ON t_resource_usage_record(request_id);
CREATE INDEX idx_resource_usage_user_id ON t_resource_usage_record(user_id);
CREATE INDEX idx_resource_usage_agent_id ON t_resource_usage_record(agent_id);
CREATE INDEX idx_resource_usage_context_id ON t_resource_usage_record(context_id);
CREATE INDEX idx_resource_usage_report_id ON t_resource_usage_record(report_id);
CREATE INDEX idx_resource_usage_task_type ON t_resource_usage_record(task_type);
CREATE INDEX idx_resource_usage_resource_type ON t_resource_usage_record(resource_type);
CREATE INDEX idx_resource_usage_create_time ON t_resource_usage_record(create_time);

-- 复合索引
CREATE INDEX idx_resource_usage_user_time ON t_resource_usage_record(user_id, create_time);
CREATE INDEX idx_resource_usage_task_resource ON t_resource_usage_record(task_type, resource_type);

-- 创建更新时间触发器
CREATE TRIGGER update_t_resource_usage_record_updated_at BEFORE UPDATE ON t_resource_usage_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
