-- Token使用记录表
CREATE TABLE t_token_usage_record (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    request_id VARCHAR(100) NOT NULL UNIQUE COMMENT '请求ID（幂等键）',
    report_id VARCHAR(50) NOT NULL COMMENT '报告ID',
    user_external_key VARCHAR(100) NOT NULL COMMENT '用户外部键',
    agent_id VARCHAR(100) NOT NULL COMMENT '智能体ID',
    session_id VARCHAR(100) COMMENT '会话ID',
    model_name VARCHAR(50) NOT NULL COMMENT '模型名称',

    input_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token数量',
    output_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token数量',
    description VARCHAR(500) COMMENT '请求描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
) COMMENT='Token使用记录表';

-- 创建索引
CREATE INDEX idx_token_usage_request_id ON t_token_usage_record(request_id);
CREATE INDEX idx_token_usage_user_key ON t_token_usage_record(user_external_key);
CREATE INDEX idx_token_usage_agent_id ON t_token_usage_record(agent_id);
CREATE INDEX idx_token_usage_create_time ON t_token_usage_record(create_time);
CREATE INDEX idx_token_usage_session_id ON t_token_usage_record(session_id);
CREATE INDEX idx_token_usage_model_name ON t_token_usage_record(model_name);
