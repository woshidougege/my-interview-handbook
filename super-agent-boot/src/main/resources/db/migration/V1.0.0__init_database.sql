-- 初始化数据库表结构
-- 使用者：Super Agent Platform v1.0.0
-- 创建时间：2024-01-01
-- 设计原则：
-- 1. 所有表名以 t_ 开头
-- 2. 使用逻辑外键，不使用物理外键约束
-- 3. 通过索引和命名规范体现关联关系

-- 1. 用户表
CREATE TABLE `t_user` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态 1-正常 0-禁用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_phone_deleted` (`phone`, `deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 用户积分账户表（汇总表）
CREATE TABLE `t_credit_account` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
    `total_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '总积分余额',
    `total_earned` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '累计获得积分',
    `total_spent` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费积分',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户表';

-- 3. 积分交易记录表
CREATE TABLE `t_credit_transaction` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
    `transaction_type` TINYINT NOT NULL COMMENT '交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零',
    `credit_type` VARCHAR(32) COMMENT '积分类型代码',
    `amount` DECIMAL(15,2) NOT NULL COMMENT '交易金额（正数表示收入，负数表示支出）',
    `balance_before` DECIMAL(15,2) NOT NULL COMMENT '交易前余额',
    `balance_after` DECIMAL(15,2) NOT NULL COMMENT '交易后余额',
    `description` VARCHAR(255) COMMENT '交易描述',
    `related_order_id` BIGINT COMMENT '关联订单ID',
    `related_subscription_id` BIGINT COMMENT '关联订阅ID（逻辑外键->t_user_subscription.id）',
    `expire_time` DATETIME COMMENT '过期时间（包月积分）',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_transaction_type` (`transaction_type`),
    INDEX `idx_credit_type` (`credit_type`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_user_deleted_time` (`user_id`, `deleted`, `create_time`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分交易记录表';

-- 4. 订阅套餐表
CREATE TABLE `t_subscription_plan` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
    `description` VARCHAR(500) COMMENT '套餐描述',
    `features` TEXT COMMENT '套餐特性描述（JSON格式）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '套餐价格（兼容字段）',
    `monthly_price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '按月价格',
    `yearly_price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '按年价格',
    `credit_amount` DECIMAL(15,2) NOT NULL COMMENT '赠送积分数量（兼容字段）',
    `monthly_credit_amount` DECIMAL(15,2) DEFAULT 0.00 COMMENT '按月赠送积分数量',
    `yearly_credit_amount` DECIMAL(15,2) DEFAULT 0.00 COMMENT '按年赠送积分数量',
    `daily_refresh_credit` INT NOT NULL DEFAULT 0 COMMENT '每日刷新积分数量',
    `validity_days` INT NOT NULL COMMENT '套餐有效期（天）',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    `is_recommended` TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐套餐 1-推荐 0-普通',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_enabled_sort` (`enabled`, `sort_order`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅套餐表';

-- 5. 用户订阅记录表
CREATE TABLE `t_user_subscription` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
    `plan_id` BIGINT NOT NULL COMMENT '套餐ID（逻辑外键->t_subscription_plan.id）',
    `start_time` DATETIME NOT NULL COMMENT '订阅开始时间',
    `end_time` DATETIME NOT NULL COMMENT '订阅结束时间',
    `paid_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `credit_amount` DECIMAL(15,2) NOT NULL COMMENT '获得积分数量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '订阅状态 1-生效中 2-已过期 3-已取消',
    `pay_order_no` VARCHAR(64) COMMENT '支付订单号',
    `remark` VARCHAR(255) COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_status_end_time` (`status`, `end_time`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_user_deleted_status` (`user_id`, `deleted`, `status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅记录表';

-- 6. 用户工作空间表
CREATE TABLE `t_user_workspace` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
    `name` VARCHAR(100) NOT NULL COMMENT '工作空间名称',
    `description` TEXT COMMENT '工作空间描述',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认工作空间：1是 0否',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2禁用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_default` (`user_id`, `is_default`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_user_deleted_default` (`user_id`, `deleted`, `is_default`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户工作空间表';

-- 7. 工作空间对话任务表
CREATE TABLE `t_workspace_chat_task` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID（逻辑外键->t_user_workspace.id）',
    `context_id` VARCHAR(100) NOT NULL COMMENT '会话上下文ID，用于与下游平台通信',
    `title` VARCHAR(200) NOT NULL COMMENT '对话任务标题',
    `content` TEXT COMMENT '对话任务内容',
    `is_favorite` TINYINT NOT NULL DEFAULT 0 COMMENT '是否收藏：1是 0否',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1进行中 2已完成 3已归档',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_workspace_id` (`workspace_id`),
    INDEX `idx_context_id` (`context_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_workspace_deleted_status` (`workspace_id`, `deleted`, `status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作空间对话任务表';


-- 初始化数据：创建默认套餐
INSERT INTO `t_subscription_plan` (
    `id`, `plan_name`, `description`, `features`, `price`, `monthly_price`, `yearly_price`, 
    `credit_amount`, `monthly_credit_amount`, `yearly_credit_amount`, `daily_refresh_credit`,
    `validity_days`, `enabled`, `is_recommended`, `sort_order`
) VALUES 
(1, '免费版', '适合轻度使用的个人用户', 
 '["新用户赠送1000积分（90天有效)","每日登录赠300积分","分享新用户奖励500积分","公共数字分身（限制体验）"]', 
 0.00, 0.00, 0.00, 1000.00, 0.00, 0.00, 0, 90, 1, 0, 1),

(2, '基础版', '适合中度使用的专业用户', 
 '["一次性获得1900永久积分","享受所有免费版权益","访问限定天","公共数字分身","幻灯片制作","网站开发","数据分析","图片、视频生成"]', 
 39.00, 39.00, 388.00, 1900.00, 1900.00, 1900.00, 0, 30, 1, 1, 2),

(3, '高级版', '适合重度使用的企业用户', 
 '["一次性获得19000永久积分","享受所有免费版权益","访问限定天","专属数字分身","幻灯片制作","网站开发","数据分析","图片、视频生成","本机电脑操控"]', 
 199.00, 199.00, 1983.00, 19000.00, 19000.00, 19000.00, 0, 30, 1, 0, 3),

(4, '额外购买积分', '直接购买永久积分，无订阅期限', 
 '["10000永久积分（无期限）","约生成10-14个PPT","约生成7-9个深度研究报告","约生成2-4个网站","约生成10-14个图片"]', 
 59.00, 59.00, 59.00, 10000.00, 10000.00, 10000.00, 0, 0, 1, 0, 4);

-- 8. 订阅订单表
CREATE TABLE `t_subscription_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` varchar(64) NOT NULL COMMENT '订单号',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `plan_id` bigint NOT NULL COMMENT '套餐ID',
    `plan_name` varchar(100) NOT NULL COMMENT '套餐名称',
    `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
    `billing_cycle` varchar(20) NOT NULL DEFAULT 'monthly' COMMENT '计费周期',
    `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '订单状态',
    `payment_method` varchar(20) DEFAULT NULL COMMENT '支付方式',
    `third_party_order_no` varchar(100) DEFAULT NULL COMMENT '第三方支付订单号',
    `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
    `expired_at` datetime NOT NULL COMMENT '过期时间',
    `effective_start_time` datetime DEFAULT NULL COMMENT '套餐生效开始时间',
    `effective_end_time` datetime DEFAULT NULL COMMENT '套餐生效结束时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅订单表';

-- 9. 支付记录表
CREATE TABLE `t_payment_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `order_no` varchar(64) NOT NULL COMMENT '订单号',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
    `payment_method` varchar(20) NOT NULL COMMENT '支付方式',
    `third_party_order_no` varchar(100) DEFAULT NULL COMMENT '第三方支付订单号',
    `third_party_transaction_no` varchar(100) DEFAULT NULL COMMENT '第三方交易流水号',
    `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '支付状态',
    `qr_code` text DEFAULT NULL COMMENT '支付二维码',
    `payment_url` varchar(500) DEFAULT NULL COMMENT '支付链接',
    `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
    `callback_data` text DEFAULT NULL COMMENT '第三方回调数据',
    `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_payment_method` (`payment_method`),
    KEY `idx_status` (`status`),
    KEY `idx_third_party_order_no` (`third_party_order_no`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- 积分类型配置表
CREATE TABLE `t_credit_type_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type_code` VARCHAR(32) NOT NULL COMMENT '积分类型代码',
    `type_name` VARCHAR(64) NOT NULL COMMENT '积分类型名称',
    `validity_days` INT NOT NULL DEFAULT 0 COMMENT '有效期天数，0表示永久',
    `consume_priority` INT NOT NULL COMMENT '消费优先级，数字越小优先级越高',
    `description` VARCHAR(200) COMMENT '描述',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type_code`),
    INDEX `idx_consume_priority` (`consume_priority`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分类型配置表';

-- 用户积分余额明细表
CREATE TABLE `t_user_credit_balance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `credit_type` VARCHAR(32) NOT NULL COMMENT '积分类型代码',
    `balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
    `total_earned` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '累计获得',
    `total_spent` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费',
    `last_earn_time` DATETIME COMMENT '最后获得时间',
    `last_spend_time` DATETIME COMMENT '最后消费时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_credit_type` (`user_id`, `credit_type`, `deleted`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_credit_type` (`credit_type`),
    INDEX `idx_balance` (`balance`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分余额明细表';

-- 积分过期清理日志表
CREATE TABLE `t_credit_expiry_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `credit_type` VARCHAR(32) NOT NULL COMMENT '积分类型代码',
    `expired_amount` DECIMAL(15,2) NOT NULL COMMENT '过期积分数量',
    `expire_date` DATE NOT NULL COMMENT '过期日期',
    `original_transaction_id` BIGINT COMMENT '原始积分交易记录ID',
    `processed_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_expire_date` (`expire_date`),
    INDEX `idx_credit_type` (`credit_type`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分过期清理日志表';

-- 初始化积分类型配置
INSERT INTO `t_credit_type_config` (`type_code`, `type_name`, `validity_days`, `consume_priority`, `description`, `enabled`) VALUES
('daily', '每日积分', 1, 1, '每日登录获得300积分，1天有效', 1),
('activity', '活动积分', 90, 2, '分享奖励等活动积分，90天有效', 1),
('new_user', '新用户积分', 90, 3, '新用户注册赠送1000积分，90天有效', 1),
('permanent', '永久积分', 0, 4, '付费购买的积分，永久有效', 1);
