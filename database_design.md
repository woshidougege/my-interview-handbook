# 数据库表设计接口文档

## 1. 概述

本文档定义了数据库表设计的相关接口和规范，用于指导新项目的数据库表结构设计。文档包含了表命名规范、字段命名规范、数据类型选择标准、基础表结构示例以及表之间的关系设计，为开发团队提供统一的数据库设计标准。

## 2. 表设计基本规范

### 2.1 命名规范
- 表名采用小写字母，多个单词用下划线分隔，如：user_profile, order_items
- 字段名采用小写字母，多个单词用下划线分隔，如：first_name, created_at
- 主键统一命名为 `id`，采用自增或雪花算法生成
- 创建时间字段命名为 `create_time`，记录数据首次创建时间
- 更新时间字段命名为 `update_time`，记录数据最后更新时间
- 外键字段命名为关联表名加_id后缀，如：user_id, role_id
- 布尔类型字段使用is_前缀，如：is_active, is_deleted
- 创建人字段命名为 `create_by`，记录创建人ID
- 更新人字段命名为 `update_by`，记录更新人ID

### 2.2 数据类型规范
- 主键ID: BIGINT UNSIGNED（64位无符号整数，支持更大数据量）
- 字符串: VARCHAR(n)（长度根据实际需要确定）或 TEXT（长文本）
- 整数: INT（32位整数）或 BIGINT（64位整数）
- 浮点数: DECIMAL(m,n)（精确小数，m为总位数，n为小数位数）
- 布尔值: TINYINT（1表示true，0表示false）
- 时间: DATETIME（日期时间）或 TIMESTAMP（时间戳）
- 枚举类型: 使用TINYINT配合注释说明，避免使用ENUM类型
- 用户ID: BIGINT UNSIGNED

## 3. 基础表结构示例

### 3.1 用户表 (user)
用户系统的核心表，存储用户基本信息和账户状态

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|-------|------|---------|-------|------|
| id | BIGINT UNSIGNED | 否 | 无 | 主键ID，自增 |
| phone | VARCHAR(20) | 否 | 无 | 手机号 |
| nickname | VARCHAR(50) | 否 | 无 | 昵称 |
| password | VARCHAR(255) | 否 | 无 | 密码（加密后） |
| status | INT | 否 | 1 | 用户状态 1-正常 0-禁用 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT UNSIGNED | 是 | 无 | 创建人ID |
| update_by | BIGINT UNSIGNED | 是 | 无 | 更新人ID |


## 4. 表关系设计

由于系统采用公司单点登录，不涉及复杂的权限管理，因此表关系相对简化。

### 4.1 用户与积分账户 (1:1)
每个用户拥有一个积分账户，通过user_id关联

### 4.2 用户与套餐订阅 (1:N)
每个用户可以订阅多个套餐，通过user_id关联

### 4.3 奋斗计划与套餐订阅 (1:N)
每个奋斗计划可以被多个用户订阅，通过plan_id关联

### 4.4 用户与工作空间 (1:1)
每个用户默认拥有一个工作空间，通过user_id关联

### 4.5 工作空间与对话任务 (1:N)
每个工作空间可以包含多个对话任务，通过workspace_id关联

## 5. 索引设计

### 5.1 唯一索引
- user表: phone（手机号唯一）

### 5.2 普通索引
- 所有create_time字段建议添加索引，便于按时间查询

## 6. SQL建表语句
-- =============================================
-- 用户表 (user)
-- =============================================
CREATE TABLE user (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
phone VARCHAR(20) NOT NULL COMMENT '手机号',
nickname VARCHAR(50) NOT NULL COMMENT '昵称',
password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
status INT NOT NULL DEFAULT 1 COMMENT '用户状态 1-正常 0-禁用',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id)
) COMMENT='用户表，存储用户基本信息和账户状态';

-- 索引
CREATE UNIQUE INDEX uk_user_phone ON user(phone);
CREATE INDEX idx_user_create_time ON user(create_time);

-- =============================================
-- 积分账户表 (user_credit_accounts)
-- =============================================
CREATE TABLE user_credit_accounts (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
user_id BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT '当前积分余额',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='用户积分账户表，管理用户积分余额和账户状态';

-- 约束
ALTER TABLE user_credit_accounts ADD CONSTRAINT chk_balance CHECK (balance >= -1000);

-- 索引
CREATE UNIQUE INDEX uk_credit_accounts_user_id ON user_credit_accounts(user_id);
CREATE INDEX idx_credit_accounts_create_time ON user_credit_accounts(create_time);

-- =============================================
-- 积分交易表 (user_credit_transactions)
-- =============================================
CREATE TABLE user_credit_transactions (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
user_id BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
type TINYINT NOT NULL COMMENT '交易类型: 1购买 2消费 3退款 4奖励 5调整',
amount DECIMAL(15, 2) NOT NULL COMMENT '交易金额（正为收入，负为支出）',
balance_before DECIMAL(15, 2) NOT NULL COMMENT '交易前余额',
balance_after DECIMAL(15, 2) NOT NULL COMMENT '交易后余额',
reference_id VARCHAR(50) COMMENT '关联业务ID（如订单ID、对话ID）',
description TEXT COMMENT '交易详细描述',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='积分交易记录表，记录所有用户积分变动历史';

-- 约束
ALTER TABLE user_credit_transactions ADD CONSTRAINT chk_balance_consistency CHECK (balance_after = balance_before + amount);

-- 索引
CREATE INDEX idx_credit_trans_user ON user_credit_transactions(user_id);
CREATE INDEX idx_credit_trans_type ON user_credit_transactions(type);
CREATE INDEX idx_credit_trans_create_time ON user_credit_transactions(create_time);

-- =============================================
-- 套餐计划表 (subscription_plans)
-- =============================================
CREATE TABLE subscription_plans (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
name VARCHAR(100) NOT NULL COMMENT '套餐名称',
code VARCHAR(50) NOT NULL COMMENT '套餐代码（唯一标识）',
description TEXT COMMENT '套餐详细描述',
price DECIMAL(10, 2) NOT NULL COMMENT '套餐价格（货币单位）',
credit_amount DECIMAL(15, 2) NOT NULL COMMENT '包含的积分数量',
duration_days INT NOT NULL COMMENT '套餐有效期（天数）',
is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1是 0否',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id)
) COMMENT='套餐计划定义表，存储平台提供的各种积分套餐方案';

-- 约束
ALTER TABLE subscription_plans ADD CONSTRAINT chk_duration_days CHECK (duration_days > 0);

-- 索引
CREATE UNIQUE INDEX uk_subscription_plans_code ON subscription_plans(code);
CREATE INDEX idx_subscription_plans_is_active ON subscription_plans(is_active);
CREATE INDEX idx_subscription_plans_create_time ON subscription_plans(create_time);

-- =============================================
-- 套餐订阅表 (user_subscriptions)
-- =============================================
CREATE TABLE user_subscriptions (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
user_id BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
plan_id BIGINT UNSIGNED NOT NULL COMMENT '关联套餐计划ID',
status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1生效中 2已取消 3已过期',
start_date DATE NOT NULL COMMENT '套餐开始日期',
end_date DATE NOT NULL COMMENT '套餐结束日期',
is_auto_renew TINYINT NOT NULL DEFAULT 1 COMMENT '是否自动续订：1是 0否',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
) COMMENT='用户套餐订阅表，管理用户购买的积分套餐订阅信息';

-- 约束
ALTER TABLE user_subscriptions ADD CONSTRAINT chk_date_range CHECK (end_date >= start_date);

-- 索引
CREATE INDEX idx_subscriptions_user ON user_subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON user_subscriptions(status);
CREATE INDEX idx_subscriptions_end_date ON user_subscriptions(end_date);
CREATE INDEX idx_subscriptions_create_time ON user_subscriptions(create_time);

-- =============================================
-- 工作空间表 (user_workspaces)
-- =============================================
CREATE TABLE user_workspaces (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
user_id BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
name VARCHAR(100) NOT NULL COMMENT '工作空间名称',
description TEXT COMMENT '工作空间描述',
is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认工作空间：1是 0否',
status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 2禁用',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id),
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='用户工作空间表，管理用户的工作空间信息';

-- 索引
CREATE UNIQUE INDEX uk_workspaces_user_default ON user_workspaces(user_id, is_default);
CREATE INDEX idx_workspaces_user ON user_workspaces(user_id);
CREATE INDEX idx_workspaces_create_time ON user_workspaces(create_time);

-- =============================================
-- 对话任务表 (workspace_chat_tasks)
-- =============================================
CREATE TABLE workspace_chat_tasks (
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
workspace_id BIGINT UNSIGNED NOT NULL COMMENT '关联工作空间ID',
session_id VARCHAR(100) NOT NULL COMMENT '会话ID，用于与下游平台通信',
title VARCHAR(200) NOT NULL COMMENT '对话任务标题',
content TEXT COMMENT '对话任务内容',
is_favorite TINYINT NOT NULL DEFAULT 0 COMMENT '是否收藏：1是 0否',
status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1进行中 2已完成 3已归档',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by BIGINT UNSIGNED COMMENT '创建人ID',
update_by BIGINT UNSIGNED COMMENT '更新人ID',
PRIMARY KEY (id),
FOREIGN KEY (workspace_id) REFERENCES user_workspaces(id) ON DELETE CASCADE
) COMMENT='对话任务表，管理工作空间中的对话任务';

-- 索引
CREATE INDEX idx_chat_tasks_workspace ON workspace_chat_tasks(workspace_id);
CREATE INDEX idx_chat_tasks_session_id ON workspace_chat_tasks(session_id);
CREATE INDEX idx_chat_tasks_status ON workspace_chat_tasks(status);
CREATE INDEX idx_chat_tasks_create_time ON workspace_chat_tasks(create_time);