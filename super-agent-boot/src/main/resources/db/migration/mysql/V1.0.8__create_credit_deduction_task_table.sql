-- 创建积分扣减任务表
-- 用于异步处理积分扣减，确保系统重启后任务不丢失

CREATE TABLE IF NOT EXISTS t_credit_deduction_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL UNIQUE COMMENT '任务ID（唯一标识）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '扣减金额',
    description VARCHAR(500) COMMENT '扣减描述',
    related_order_id BIGINT COMMENT '关联订单ID',
    resource_usage_record_id BIGINT COMMENT '关联的资源使用记录ID',
    
    -- 任务状态和重试信息
    status TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0-待处理，1-处理中，2-成功，3-失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    
    -- 错误信息
    error_message TEXT COMMENT '错误信息',
    last_error_time DATETIME COMMENT '最后错误时间',
    
    -- 时间字段
    scheduled_time DATETIME NOT NULL COMMENT '计划执行时间',
    executed_time DATETIME COMMENT '实际执行时间',
    completed_time DATETIME COMMENT '完成时间',
    
    -- 标准审计字段
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_create_time (create_time),
    INDEX idx_resource_usage_record_id (resource_usage_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分扣减任务表';
