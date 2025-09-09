-- 优化积分账户表结构
-- 创建时间：2024-01-03
-- 说明：根据积分规则优化积分分类，支持按有效期管理和扣费

-- 1. 添加新的积分分类字段到用户积分账户表
ALTER TABLE `t_credit_account` 
ADD COLUMN `daily_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '当日积分余额（1天有效）' AFTER `subscription_balance`,
ADD COLUMN `activity_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '活动积分余额（90天有效，如分享奖励）' AFTER `daily_balance`,
ADD COLUMN `permanent_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '永久积分余额（付费积分，无期限）' AFTER `activity_balance`;

-- 2. 更新字段注释，明确积分分类
ALTER TABLE `t_credit_account` 
MODIFY COLUMN `free_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '免费积分余额（新用户1000积分，90天有效）',
MODIFY COLUMN `subscription_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '包月积分余额（已废弃，数据迁移后可删除）';

-- 3. 创建积分过期清理日志表
CREATE TABLE IF NOT EXISTS `t_credit_expiry_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `credit_type` VARCHAR(20) NOT NULL COMMENT '积分类型：daily/activity/free',
    `expired_amount` DECIMAL(15,2) NOT NULL COMMENT '过期积分数量',
    `expire_date` DATE NOT NULL COMMENT '过期日期',
    `original_transaction_id` BIGINT COMMENT '原始积分交易记录ID',
    `processed_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_expire_date` (`expire_date`),
    INDEX `idx_credit_type` (`credit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分过期清理日志表';

-- 4. 添加积分消费顺序配置表（用于灵活配置扣费顺序）
CREATE TABLE IF NOT EXISTS `t_credit_consume_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `credit_type` VARCHAR(20) NOT NULL COMMENT '积分类型',
    `priority` INT NOT NULL COMMENT '优先级（数字越小优先级越高）',
    `description` VARCHAR(100) COMMENT '描述',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credit_type` (`credit_type`),
    INDEX `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分消费顺序配置表';

-- 5. 初始化积分消费顺序配置
INSERT INTO `t_credit_consume_order` (`credit_type`, `priority`, `description`) VALUES
('daily', 1, '当日积分（1天有效）'),
('activity', 2, '活动积分（90天有效，如分享奖励）'),
('free', 3, '免费积分（新用户1000积分，90天有效）'),
('permanent', 4, '永久积分（付费积分，无期限）');
