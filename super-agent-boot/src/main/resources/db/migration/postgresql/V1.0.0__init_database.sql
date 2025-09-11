-- 初始化数据库表结构（PostgreSQL版本）
-- 使用者：Super Agent Platform v1.0.0
-- 创建时间：2024-01-01
-- 设计原则：
-- 1. 所有表名以 t_ 开头
-- 2. 使用逻辑外键，不使用物理外键约束
-- 3. 通过索引和命名规范体现关联关系

-- 1. 用户表
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

-- 用户表注释
COMMENT ON TABLE t_user IS '用户表';
COMMENT ON COLUMN t_user.id IS '主键ID';
COMMENT ON COLUMN t_user.phone IS '手机号';
COMMENT ON COLUMN t_user.username IS '用户名';
COMMENT ON COLUMN t_user.password IS '密码（加密后）';
COMMENT ON COLUMN t_user.status IS '用户状态 1-正常 0-禁用';
COMMENT ON COLUMN t_user.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_user.create_time IS '创建时间';
COMMENT ON COLUMN t_user.update_time IS '更新时间';
COMMENT ON COLUMN t_user.create_by IS '创建人ID';
COMMENT ON COLUMN t_user.update_by IS '更新人ID';

-- 用户表索引
CREATE UNIQUE INDEX uk_user_phone ON t_user(phone);
CREATE INDEX idx_user_status ON t_user(status);
CREATE INDEX idx_user_deleted ON t_user(deleted);
CREATE INDEX idx_user_phone_deleted ON t_user(phone, deleted);
CREATE INDEX idx_user_create_time ON t_user(create_time);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为用户表创建更新时间触发器
CREATE TRIGGER update_t_user_updated_at BEFORE UPDATE ON t_user
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 2. 用户积分账户表（汇总表）
CREATE TABLE t_credit_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    version INTEGER NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_credit_account IS '用户积分账户表';
COMMENT ON COLUMN t_credit_account.id IS '主键ID';
COMMENT ON COLUMN t_credit_account.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN t_credit_account.total_balance IS '总积分余额';
COMMENT ON COLUMN t_credit_account.total_earned IS '累计获得积分';
COMMENT ON COLUMN t_credit_account.total_spent IS '累计消费积分';
COMMENT ON COLUMN t_credit_account.version IS '版本号（乐观锁）';
COMMENT ON COLUMN t_credit_account.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_credit_account.create_time IS '创建时间';
COMMENT ON COLUMN t_credit_account.update_time IS '更新时间';
COMMENT ON COLUMN t_credit_account.create_by IS '创建人ID';
COMMENT ON COLUMN t_credit_account.update_by IS '更新人ID';

CREATE UNIQUE INDEX uk_credit_account_user_id ON t_credit_account(user_id);
CREATE INDEX idx_credit_account_deleted ON t_credit_account(deleted);
CREATE INDEX idx_credit_account_create_time ON t_credit_account(create_time);
CREATE TRIGGER update_t_credit_account_updated_at BEFORE UPDATE ON t_credit_account
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. 积分交易记录表
CREATE TABLE t_credit_transaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_type SMALLINT NOT NULL,
    credit_type VARCHAR(32),
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    description VARCHAR(255),
    related_order_id BIGINT,
    related_subscription_id BIGINT,
    expire_time TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_credit_transaction IS '积分交易记录表';
COMMENT ON COLUMN t_credit_transaction.id IS '主键ID';
COMMENT ON COLUMN t_credit_transaction.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN t_credit_transaction.transaction_type IS '交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零';
COMMENT ON COLUMN t_credit_transaction.credit_type IS '积分类型代码';
COMMENT ON COLUMN t_credit_transaction.amount IS '交易金额（正数表示收入，负数表示支出）';
COMMENT ON COLUMN t_credit_transaction.balance_before IS '交易前余额';
COMMENT ON COLUMN t_credit_transaction.balance_after IS '交易后余额';
COMMENT ON COLUMN t_credit_transaction.description IS '交易描述';
COMMENT ON COLUMN t_credit_transaction.related_order_id IS '关联订单ID';
COMMENT ON COLUMN t_credit_transaction.related_subscription_id IS '关联订阅ID（逻辑外键->t_user_subscription.id）';
COMMENT ON COLUMN t_credit_transaction.expire_time IS '过期时间（包月积分）';
COMMENT ON COLUMN t_credit_transaction.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_credit_transaction.create_time IS '创建时间';
COMMENT ON COLUMN t_credit_transaction.update_time IS '更新时间';
COMMENT ON COLUMN t_credit_transaction.create_by IS '创建人ID';
COMMENT ON COLUMN t_credit_transaction.update_by IS '更新人ID';

CREATE INDEX idx_credit_transaction_user_id ON t_credit_transaction(user_id);
CREATE INDEX idx_credit_transaction_type ON t_credit_transaction(transaction_type);
CREATE INDEX idx_credit_transaction_credit_type ON t_credit_transaction(credit_type);
CREATE INDEX idx_credit_transaction_deleted ON t_credit_transaction(deleted);
CREATE INDEX idx_credit_transaction_user_deleted_time ON t_credit_transaction(user_id, deleted, create_time);
CREATE INDEX idx_credit_transaction_create_time ON t_credit_transaction(create_time);
CREATE TRIGGER update_t_credit_transaction_updated_at BEFORE UPDATE ON t_credit_transaction
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 4. 订阅套餐表
CREATE TABLE t_subscription_plan (
    id BIGSERIAL PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    features TEXT,
    price DECIMAL(10,2) NOT NULL,
    monthly_price DECIMAL(10,2) DEFAULT 0.00,
    yearly_price DECIMAL(10,2) DEFAULT 0.00,
    credit_amount DECIMAL(15,2) NOT NULL,
    monthly_credit_amount DECIMAL(15,2) DEFAULT 0.00,
    yearly_credit_amount DECIMAL(15,2) DEFAULT 0.00,
    daily_refresh_credit INTEGER NOT NULL DEFAULT 0,
    validity_days INTEGER NOT NULL,
    enabled SMALLINT NOT NULL DEFAULT 1,
    is_recommended SMALLINT NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_subscription_plan IS '订阅套餐表';
COMMENT ON COLUMN t_subscription_plan.id IS '主键ID';
COMMENT ON COLUMN t_subscription_plan.plan_name IS '套餐名称';
COMMENT ON COLUMN t_subscription_plan.description IS '套餐描述';
COMMENT ON COLUMN t_subscription_plan.features IS '套餐特性描述（JSON格式）';
COMMENT ON COLUMN t_subscription_plan.price IS '套餐价格（兼容字段）';
COMMENT ON COLUMN t_subscription_plan.monthly_price IS '按月价格';
COMMENT ON COLUMN t_subscription_plan.yearly_price IS '按年价格';
COMMENT ON COLUMN t_subscription_plan.credit_amount IS '赠送积分数量（兼容字段）';
COMMENT ON COLUMN t_subscription_plan.monthly_credit_amount IS '按月赠送积分数量';
COMMENT ON COLUMN t_subscription_plan.yearly_credit_amount IS '按年赠送积分数量';
COMMENT ON COLUMN t_subscription_plan.daily_refresh_credit IS '每日刷新积分数量';
COMMENT ON COLUMN t_subscription_plan.validity_days IS '套餐有效期（天）';
COMMENT ON COLUMN t_subscription_plan.enabled IS '是否启用 1-启用 0-禁用';
COMMENT ON COLUMN t_subscription_plan.is_recommended IS '是否推荐套餐 1-推荐 0-普通';
COMMENT ON COLUMN t_subscription_plan.sort_order IS '排序值';
COMMENT ON COLUMN t_subscription_plan.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_subscription_plan.create_time IS '创建时间';
COMMENT ON COLUMN t_subscription_plan.update_time IS '更新时间';
COMMENT ON COLUMN t_subscription_plan.create_by IS '创建人ID';
COMMENT ON COLUMN t_subscription_plan.update_by IS '更新人ID';

CREATE INDEX idx_subscription_plan_enabled_sort ON t_subscription_plan(enabled, sort_order);
CREATE INDEX idx_subscription_plan_deleted ON t_subscription_plan(deleted);
CREATE INDEX idx_subscription_plan_create_time ON t_subscription_plan(create_time);
CREATE TRIGGER update_t_subscription_plan_updated_at BEFORE UPDATE ON t_subscription_plan
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 5. 用户订阅记录表
CREATE TABLE t_user_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL,
    credit_amount DECIMAL(15,2) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    pay_order_no VARCHAR(64),
    remark VARCHAR(255),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_user_subscription IS '用户订阅记录表';
COMMENT ON COLUMN t_user_subscription.id IS '主键ID';
COMMENT ON COLUMN t_user_subscription.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN t_user_subscription.plan_id IS '套餐ID（逻辑外键->t_subscription_plan.id）';
COMMENT ON COLUMN t_user_subscription.start_time IS '订阅开始时间';
COMMENT ON COLUMN t_user_subscription.end_time IS '订阅结束时间';
COMMENT ON COLUMN t_user_subscription.paid_amount IS '支付金额';
COMMENT ON COLUMN t_user_subscription.credit_amount IS '获得积分数量';
COMMENT ON COLUMN t_user_subscription.status IS '订阅状态 1-生效中 2-已过期 3-已取消';
COMMENT ON COLUMN t_user_subscription.pay_order_no IS '支付订单号';
COMMENT ON COLUMN t_user_subscription.remark IS '备注';
COMMENT ON COLUMN t_user_subscription.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_user_subscription.create_time IS '创建时间';
COMMENT ON COLUMN t_user_subscription.update_time IS '更新时间';
COMMENT ON COLUMN t_user_subscription.create_by IS '创建人ID';
COMMENT ON COLUMN t_user_subscription.update_by IS '更新人ID';

CREATE INDEX idx_user_subscription_user_id ON t_user_subscription(user_id);
CREATE INDEX idx_user_subscription_plan_id ON t_user_subscription(plan_id);
CREATE INDEX idx_user_subscription_status_end_time ON t_user_subscription(status, end_time);
CREATE INDEX idx_user_subscription_deleted ON t_user_subscription(deleted);
CREATE INDEX idx_user_subscription_user_deleted_status ON t_user_subscription(user_id, deleted, status);
CREATE INDEX idx_user_subscription_create_time ON t_user_subscription(create_time);
CREATE TRIGGER update_t_user_subscription_updated_at BEFORE UPDATE ON t_user_subscription
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 6. 用户工作空间表
CREATE TABLE t_user_workspace (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default SMALLINT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_user_workspace IS '用户工作空间表';
COMMENT ON COLUMN t_user_workspace.id IS '主键ID';
COMMENT ON COLUMN t_user_workspace.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN t_user_workspace.name IS '工作空间名称';
COMMENT ON COLUMN t_user_workspace.description IS '工作空间描述';
COMMENT ON COLUMN t_user_workspace.is_default IS '是否默认工作空间：1是 0否';
COMMENT ON COLUMN t_user_workspace.status IS '状态: 1正常 2禁用';
COMMENT ON COLUMN t_user_workspace.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_user_workspace.create_time IS '创建时间';
COMMENT ON COLUMN t_user_workspace.update_time IS '更新时间';
COMMENT ON COLUMN t_user_workspace.create_by IS '创建人ID';
COMMENT ON COLUMN t_user_workspace.update_by IS '更新人ID';

CREATE INDEX idx_user_workspace_user_id ON t_user_workspace(user_id);
CREATE INDEX idx_user_workspace_user_default ON t_user_workspace(user_id, is_default);
CREATE INDEX idx_user_workspace_deleted ON t_user_workspace(deleted);
CREATE INDEX idx_user_workspace_user_deleted_default ON t_user_workspace(user_id, deleted, is_default);
CREATE INDEX idx_user_workspace_create_time ON t_user_workspace(create_time);
CREATE TRIGGER update_t_user_workspace_updated_at BEFORE UPDATE ON t_user_workspace
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 7. 工作空间对话任务表
CREATE TABLE t_workspace_chat_task (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    context_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    is_favorite SMALLINT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_workspace_chat_task IS '工作空间对话任务表';
COMMENT ON COLUMN t_workspace_chat_task.id IS '主键ID';
COMMENT ON COLUMN t_workspace_chat_task.workspace_id IS '工作空间ID（逻辑外键->t_user_workspace.id）';
COMMENT ON COLUMN t_workspace_chat_task.context_id IS '会话上下文ID，用于与下游平台通信';
COMMENT ON COLUMN t_workspace_chat_task.title IS '对话任务标题';
COMMENT ON COLUMN t_workspace_chat_task.content IS '对话任务内容';
COMMENT ON COLUMN t_workspace_chat_task.is_favorite IS '是否收藏：1是 0否';
COMMENT ON COLUMN t_workspace_chat_task.status IS '状态: 1进行中 2已完成 3已归档';
COMMENT ON COLUMN t_workspace_chat_task.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_workspace_chat_task.create_time IS '创建时间';
COMMENT ON COLUMN t_workspace_chat_task.update_time IS '更新时间';
COMMENT ON COLUMN t_workspace_chat_task.create_by IS '创建人ID';
COMMENT ON COLUMN t_workspace_chat_task.update_by IS '更新人ID';

CREATE INDEX idx_workspace_chat_task_workspace_id ON t_workspace_chat_task(workspace_id);
CREATE INDEX idx_workspace_chat_task_context_id ON t_workspace_chat_task(context_id);
CREATE INDEX idx_workspace_chat_task_status ON t_workspace_chat_task(status);
CREATE INDEX idx_workspace_chat_task_deleted ON t_workspace_chat_task(deleted);
CREATE INDEX idx_workspace_chat_task_workspace_deleted_status ON t_workspace_chat_task(workspace_id, deleted, status);
CREATE INDEX idx_workspace_chat_task_create_time ON t_workspace_chat_task(create_time);
CREATE TRIGGER update_t_workspace_chat_task_updated_at BEFORE UPDATE ON t_workspace_chat_task
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 初始化数据：创建默认套餐
INSERT INTO t_subscription_plan (
    id, plan_name, description, features, price, monthly_price, yearly_price, 
    credit_amount, monthly_credit_amount, yearly_credit_amount, daily_refresh_credit,
    validity_days, enabled, is_recommended, sort_order
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
CREATE TABLE t_subscription_order (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'monthly',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    payment_method VARCHAR(20),
    third_party_order_no VARCHAR(100),
    paid_at TIMESTAMP,
    expired_at TIMESTAMP NOT NULL,
    effective_start_time TIMESTAMP,
    effective_end_time TIMESTAMP,
    remark VARCHAR(500),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_subscription_order IS '订阅订单表';
COMMENT ON COLUMN t_subscription_order.id IS '主键ID';
COMMENT ON COLUMN t_subscription_order.order_no IS '订单号';
COMMENT ON COLUMN t_subscription_order.user_id IS '用户ID';
COMMENT ON COLUMN t_subscription_order.plan_id IS '套餐ID';
COMMENT ON COLUMN t_subscription_order.plan_name IS '套餐名称';
COMMENT ON COLUMN t_subscription_order.amount IS '订单金额';
COMMENT ON COLUMN t_subscription_order.billing_cycle IS '计费周期';
COMMENT ON COLUMN t_subscription_order.status IS '订单状态';
COMMENT ON COLUMN t_subscription_order.payment_method IS '支付方式';
COMMENT ON COLUMN t_subscription_order.third_party_order_no IS '第三方支付订单号';
COMMENT ON COLUMN t_subscription_order.paid_at IS '支付时间';
COMMENT ON COLUMN t_subscription_order.expired_at IS '过期时间';
COMMENT ON COLUMN t_subscription_order.effective_start_time IS '套餐生效开始时间';
COMMENT ON COLUMN t_subscription_order.effective_end_time IS '套餐生效结束时间';
COMMENT ON COLUMN t_subscription_order.remark IS '备注';
COMMENT ON COLUMN t_subscription_order.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_subscription_order.create_time IS '创建时间';
COMMENT ON COLUMN t_subscription_order.update_time IS '更新时间';
COMMENT ON COLUMN t_subscription_order.create_by IS '创建人ID';
COMMENT ON COLUMN t_subscription_order.update_by IS '更新人ID';

CREATE UNIQUE INDEX uk_subscription_order_order_no ON t_subscription_order(order_no);
CREATE INDEX idx_subscription_order_user_id ON t_subscription_order(user_id);
CREATE INDEX idx_subscription_order_plan_id ON t_subscription_order(plan_id);
CREATE INDEX idx_subscription_order_status ON t_subscription_order(status);
CREATE INDEX idx_subscription_order_deleted ON t_subscription_order(deleted);
CREATE INDEX idx_subscription_order_create_time ON t_subscription_order(create_time);
CREATE TRIGGER update_t_subscription_order_updated_at BEFORE UPDATE ON t_subscription_order
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 9. 支付记录表
CREATE TABLE t_payment_record (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    third_party_order_no VARCHAR(100),
    third_party_transaction_no VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    qr_code TEXT,
    payment_url VARCHAR(500),
    paid_at TIMESTAMP,
    callback_data TEXT,
    failure_reason VARCHAR(500),
    remark VARCHAR(500),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_payment_record IS '支付记录表';
COMMENT ON COLUMN t_payment_record.id IS '主键ID';
COMMENT ON COLUMN t_payment_record.order_id IS '订单ID';
COMMENT ON COLUMN t_payment_record.order_no IS '订单号';
COMMENT ON COLUMN t_payment_record.user_id IS '用户ID';
COMMENT ON COLUMN t_payment_record.amount IS '支付金额';
COMMENT ON COLUMN t_payment_record.payment_method IS '支付方式';
COMMENT ON COLUMN t_payment_record.third_party_order_no IS '第三方支付订单号';
COMMENT ON COLUMN t_payment_record.third_party_transaction_no IS '第三方交易流水号';
COMMENT ON COLUMN t_payment_record.status IS '支付状态';
COMMENT ON COLUMN t_payment_record.qr_code IS '支付二维码';
COMMENT ON COLUMN t_payment_record.payment_url IS '支付链接';
COMMENT ON COLUMN t_payment_record.paid_at IS '支付时间';
COMMENT ON COLUMN t_payment_record.callback_data IS '第三方回调数据';
COMMENT ON COLUMN t_payment_record.failure_reason IS '失败原因';
COMMENT ON COLUMN t_payment_record.remark IS '备注';
COMMENT ON COLUMN t_payment_record.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_payment_record.create_time IS '创建时间';
COMMENT ON COLUMN t_payment_record.update_time IS '更新时间';
COMMENT ON COLUMN t_payment_record.create_by IS '创建人ID';
COMMENT ON COLUMN t_payment_record.update_by IS '更新人ID';

CREATE INDEX idx_payment_record_order_id ON t_payment_record(order_id);
CREATE INDEX idx_payment_record_order_no ON t_payment_record(order_no);
CREATE INDEX idx_payment_record_user_id ON t_payment_record(user_id);
CREATE INDEX idx_payment_record_payment_method ON t_payment_record(payment_method);
CREATE INDEX idx_payment_record_status ON t_payment_record(status);
CREATE INDEX idx_payment_record_third_party_order_no ON t_payment_record(third_party_order_no);
CREATE INDEX idx_payment_record_deleted ON t_payment_record(deleted);
CREATE INDEX idx_payment_record_create_time ON t_payment_record(create_time);
CREATE TRIGGER update_t_payment_record_updated_at BEFORE UPDATE ON t_payment_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 积分类型配置表
CREATE TABLE t_credit_type_config (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(32) NOT NULL,
    type_name VARCHAR(64) NOT NULL,
    validity_days INTEGER NOT NULL DEFAULT 0,
    consume_priority INTEGER NOT NULL,
    description VARCHAR(200),
    enabled SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_credit_type_config IS '积分类型配置表';
COMMENT ON COLUMN t_credit_type_config.id IS '主键ID';
COMMENT ON COLUMN t_credit_type_config.type_code IS '积分类型代码';
COMMENT ON COLUMN t_credit_type_config.type_name IS '积分类型名称';
COMMENT ON COLUMN t_credit_type_config.validity_days IS '有效期天数，0表示永久';
COMMENT ON COLUMN t_credit_type_config.consume_priority IS '消费优先级，数字越小优先级越高';
COMMENT ON COLUMN t_credit_type_config.description IS '描述';
COMMENT ON COLUMN t_credit_type_config.enabled IS '是否启用';
COMMENT ON COLUMN t_credit_type_config.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_credit_type_config.create_time IS '创建时间';
COMMENT ON COLUMN t_credit_type_config.update_time IS '更新时间';
COMMENT ON COLUMN t_credit_type_config.create_by IS '创建人ID';
COMMENT ON COLUMN t_credit_type_config.update_by IS '更新人ID';

CREATE UNIQUE INDEX uk_credit_type_config_type_code ON t_credit_type_config(type_code);
CREATE INDEX idx_credit_type_config_consume_priority ON t_credit_type_config(consume_priority);
CREATE INDEX idx_credit_type_config_enabled ON t_credit_type_config(enabled);
CREATE INDEX idx_credit_type_config_deleted ON t_credit_type_config(deleted);
CREATE INDEX idx_credit_type_config_create_time ON t_credit_type_config(create_time);
CREATE TRIGGER update_t_credit_type_config_updated_at BEFORE UPDATE ON t_credit_type_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 用户积分余额明细表
CREATE TABLE t_user_credit_balance (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credit_type VARCHAR(32) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    last_earn_time TIMESTAMP,
    last_spend_time TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_user_credit_balance IS '用户积分余额明细表';
COMMENT ON COLUMN t_user_credit_balance.id IS '主键ID';
COMMENT ON COLUMN t_user_credit_balance.user_id IS '用户ID';
COMMENT ON COLUMN t_user_credit_balance.credit_type IS '积分类型代码';
COMMENT ON COLUMN t_user_credit_balance.balance IS '余额';
COMMENT ON COLUMN t_user_credit_balance.total_earned IS '累计获得';
COMMENT ON COLUMN t_user_credit_balance.total_spent IS '累计消费';
COMMENT ON COLUMN t_user_credit_balance.last_earn_time IS '最后获得时间';
COMMENT ON COLUMN t_user_credit_balance.last_spend_time IS '最后消费时间';
COMMENT ON COLUMN t_user_credit_balance.version IS '版本号（乐观锁）';
COMMENT ON COLUMN t_user_credit_balance.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_user_credit_balance.create_time IS '创建时间';
COMMENT ON COLUMN t_user_credit_balance.update_time IS '更新时间';
COMMENT ON COLUMN t_user_credit_balance.create_by IS '创建人ID';
COMMENT ON COLUMN t_user_credit_balance.update_by IS '更新人ID';

CREATE UNIQUE INDEX uk_user_credit_balance_user_credit_type ON t_user_credit_balance(user_id, credit_type, deleted);
CREATE INDEX idx_user_credit_balance_user_id ON t_user_credit_balance(user_id);
CREATE INDEX idx_user_credit_balance_credit_type ON t_user_credit_balance(credit_type);
CREATE INDEX idx_user_credit_balance_balance ON t_user_credit_balance(balance);
CREATE INDEX idx_user_credit_balance_deleted ON t_user_credit_balance(deleted);
CREATE INDEX idx_user_credit_balance_create_time ON t_user_credit_balance(create_time);
CREATE TRIGGER update_t_user_credit_balance_updated_at BEFORE UPDATE ON t_user_credit_balance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 积分过期清理日志表
CREATE TABLE t_credit_expiry_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credit_type VARCHAR(32) NOT NULL,
    expired_amount DECIMAL(15,2) NOT NULL,
    expire_date DATE NOT NULL,
    original_transaction_id BIGINT,
    processed_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE t_credit_expiry_log IS '积分过期清理日志表';
COMMENT ON COLUMN t_credit_expiry_log.id IS '主键ID';
COMMENT ON COLUMN t_credit_expiry_log.user_id IS '用户ID';
COMMENT ON COLUMN t_credit_expiry_log.credit_type IS '积分类型代码';
COMMENT ON COLUMN t_credit_expiry_log.expired_amount IS '过期积分数量';
COMMENT ON COLUMN t_credit_expiry_log.expire_date IS '过期日期';
COMMENT ON COLUMN t_credit_expiry_log.original_transaction_id IS '原始积分交易记录ID';
COMMENT ON COLUMN t_credit_expiry_log.processed_time IS '处理时间';
COMMENT ON COLUMN t_credit_expiry_log.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN t_credit_expiry_log.create_time IS '创建时间';
COMMENT ON COLUMN t_credit_expiry_log.update_time IS '更新时间';
COMMENT ON COLUMN t_credit_expiry_log.create_by IS '创建人ID';
COMMENT ON COLUMN t_credit_expiry_log.update_by IS '更新人ID';

CREATE INDEX idx_credit_expiry_log_user_id ON t_credit_expiry_log(user_id);
CREATE INDEX idx_credit_expiry_log_expire_date ON t_credit_expiry_log(expire_date);
CREATE INDEX idx_credit_expiry_log_credit_type ON t_credit_expiry_log(credit_type);
CREATE INDEX idx_credit_expiry_log_deleted ON t_credit_expiry_log(deleted);
CREATE INDEX idx_credit_expiry_log_create_time ON t_credit_expiry_log(create_time);
CREATE TRIGGER update_t_credit_expiry_log_updated_at BEFORE UPDATE ON t_credit_expiry_log
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 初始化积分类型配置
INSERT INTO t_credit_type_config (type_code, type_name, validity_days, consume_priority, description, enabled) VALUES
('daily', '每日积分', 1, 1, '每日登录获得300积分，1天有效', 1),
('activity', '活动积分', 90, 2, '分享奖励等活动积分，90天有效', 1),
('new_user', '新用户积分', 90, 3, '新用户注册赠送1000积分，90天有效', 1),
('permanent', '永久积分', 0, 4, '付费购买的积分，永久有效', 1);
