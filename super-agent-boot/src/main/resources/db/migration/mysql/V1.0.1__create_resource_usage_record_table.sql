-- 资源使用记录表
CREATE TABLE t_resource_usage_record (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    request_id VARCHAR(100) NOT NULL COMMENT '请求ID（幂等键）',
    report_id VARCHAR(50) NOT NULL COMMENT '报告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID（雪花算法生成）',
    agent_id VARCHAR(100) NOT NULL COMMENT '智能体ID',
    context_id VARCHAR(100) COMMENT '会话ID',
    
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型：INDUSTRY_RESEARCH_REPORT, PPT_GENERATION等',
    task_description VARCHAR(500) COMMENT '任务描述',
    
    resource_type VARCHAR(20) NOT NULL COMMENT '资源类型：MODEL, FUNCTION, MEDIA',
    resource_name VARCHAR(100) NOT NULL COMMENT '资源名称：模型名称、功能类型、媒体类型等',
    resource_subtype VARCHAR(50) COMMENT '资源子类型：TEXT_GENERATION, IMAGE_GENERATION等',
    
    usage_data TEXT COMMENT '使用量JSON数据（Token数、次数、秒数等）',
    billing_unit VARCHAR(20) COMMENT '计费单位：TOKEN, TIMES, PAGES, COUNT, SECONDS',
    usage_amount DECIMAL(15,6) DEFAULT 0 COMMENT '使用量',
    unit_price DECIMAL(10,6) DEFAULT 0 COMMENT '单价（元）',
    billing_amount DECIMAL(15,6) DEFAULT 0 COMMENT '计费金额（元）',
    
    description VARCHAR(500) COMMENT '描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
) COMMENT='资源使用记录表';

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
