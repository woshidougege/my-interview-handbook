-- 初始化数据库表结构
-- 使用者：Super Agent Platform v1.0.0
-- 创建时间：2024-01-01

-- 1. 用户表（简化版）
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态 1-正常 0-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 积分账户表
CREATE TABLE `credit_account` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总积分余额',
    `free_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '免费积分余额',
    `subscription_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '包月积分余额',
    `total_earned` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计获得积分',
    `total_spent` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费积分',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_credit_account_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户表';

-- 3. 积分交易记录表
CREATE TABLE `credit_transaction` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `transaction_type` TINYINT NOT NULL COMMENT '交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '交易金额（正数表示收入，负数表示支出）',
    `balance_before` DECIMAL(10,2) NOT NULL COMMENT '交易前余额',
    `balance_after` DECIMAL(10,2) NOT NULL COMMENT '交易后余额',
    `description` VARCHAR(255) COMMENT '交易描述',
    `related_order_id` BIGINT COMMENT '关联订单ID',
    `related_subscription_id` BIGINT COMMENT '关联订阅ID',
    `expire_time` DATETIME COMMENT '过期时间（包月积分）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_transaction_type` (`transaction_type`),
    INDEX `idx_create_time` (`create_time`),
    CONSTRAINT `fk_credit_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分交易记录表';

-- 4. 订阅套餐表
CREATE TABLE `subscription_plan` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
    `description` VARCHAR(500) COMMENT '套餐描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '套餐价格',
    `credit_amount` DECIMAL(10,2) NOT NULL COMMENT '赠送积分数量',
    `validity_days` INT NOT NULL COMMENT '套餐有效期（天）',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅套餐表';

-- 5. 用户订阅记录表
CREATE TABLE `user_subscription` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
    `start_time` DATETIME NOT NULL COMMENT '订阅开始时间',
    `end_time` DATETIME NOT NULL COMMENT '订阅结束时间',
    `paid_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `credit_amount` DECIMAL(10,2) NOT NULL COMMENT '获得积分数量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '订阅状态 1-生效中 2-已过期 3-已取消',
    `pay_order_no` VARCHAR(64) COMMENT '支付订单号',
    `remark` VARCHAR(255) COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status_end_time` (`status`, `end_time`),
    CONSTRAINT `fk_user_subscription_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_user_subscription_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅记录表';

-- 6. db-scheduler调度表
CREATE TABLE `scheduled_tasks` (
    `task_name` VARCHAR(40) NOT NULL,
    `task_instance` VARCHAR(40) NOT NULL,
    `task_data` BLOB,
    `execution_time` TIMESTAMP(6) NOT NULL,
    `picked` TINYINT(1) NOT NULL,
    `picked_by` VARCHAR(50),
    `last_success` TIMESTAMP(6) NULL,
    `last_failure` TIMESTAMP(6) NULL,
    `consecutive_failures` INT,
    `last_heartbeat` TIMESTAMP(6) NULL,
    `version` BIGINT NOT NULL,
    PRIMARY KEY (`task_name`, `task_instance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务调度表';

-- 初始化数据：创建默认套餐
INSERT INTO `subscription_plan` (`id`, `plan_name`, `description`, `price`, `credit_amount`, `validity_days`, `enabled`, `sort_order`) VALUES 
(1, '基础套餐', '每月基础积分套餐', 29.90, 1000.00, 30, 1, 1),
(2, '标准套餐', '每月标准积分套餐', 99.90, 5000.00, 30, 1, 2),
(3, '高级套餐', '每月高级积分套餐', 199.90, 12000.00, 30, 1, 3);
