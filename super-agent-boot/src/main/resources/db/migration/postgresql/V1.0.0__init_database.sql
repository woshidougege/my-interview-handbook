-- public.flyway_schema_history definition

-- Drop table

-- DROP TABLE flyway_schema_history;

CREATE TABLE flyway_schema_history (
	installed_rank int4 NOT NULL,
	"version" varchar(50) NULL,
	description varchar(200) NOT NULL,
	"type" varchar(20) NOT NULL,
	script varchar(1000) NOT NULL,
	checksum int4 NULL,
	installed_by varchar(100) NOT NULL,
	installed_on timestamp DEFAULT now() NOT NULL,
	execution_time int4 NOT NULL,
	success bool NOT NULL,
	CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


-- public.t_credit_account definition

-- Drop table

-- DROP TABLE t_credit_account;

CREATE TABLE t_credit_account (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID（逻辑外键->t_user.id）
	total_balance numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 总积分余额
	total_earned numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 累计获得积分
	total_spent numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 累计消费积分
	"version" int4 DEFAULT 0 NOT NULL, -- 版本号（乐观锁）
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_credit_account_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_credit_account_create_time ON public.t_credit_account USING btree (create_time);
CREATE INDEX idx_credit_account_deleted ON public.t_credit_account USING btree (deleted);
CREATE UNIQUE INDEX uk_credit_account_user_id ON public.t_credit_account USING btree (user_id);
COMMENT ON TABLE public.t_credit_account IS '用户积分账户表';

-- Column comments

COMMENT ON COLUMN public.t_credit_account.id IS '主键ID';
COMMENT ON COLUMN public.t_credit_account.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN public.t_credit_account.total_balance IS '总积分余额';
COMMENT ON COLUMN public.t_credit_account.total_earned IS '累计获得积分';
COMMENT ON COLUMN public.t_credit_account.total_spent IS '累计消费积分';
COMMENT ON COLUMN public.t_credit_account."version" IS '版本号（乐观锁）';
COMMENT ON COLUMN public.t_credit_account.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_credit_account.create_time IS '创建时间';
COMMENT ON COLUMN public.t_credit_account.update_time IS '更新时间';
COMMENT ON COLUMN public.t_credit_account.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_credit_account.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_credit_account_updated_at before
update
    on
    public.t_credit_account for each row execute function update_updated_at_column();


-- public.t_credit_deduction_task definition

-- Drop table

-- DROP TABLE t_credit_deduction_task;

CREATE TABLE t_credit_deduction_task (
	id bigserial NOT NULL, -- 主键ID
	task_id varchar(64) NOT NULL, -- 任务ID（唯一标识）
	user_id int8 NOT NULL, -- 用户ID
	amount numeric(10, 2) NOT NULL, -- 扣减金额
	description varchar(500) NULL, -- 扣减描述
	related_order_id int8 NULL, -- 关联订单ID
	resource_usage_record_id int8 NULL, -- 关联的资源使用记录ID
	status int2 DEFAULT 0 NOT NULL, -- 任务状态：0-待处理，1-处理中，2-成功，3-失败
	retry_count int4 DEFAULT 0 NOT NULL, -- 重试次数
	max_retry_count int4 DEFAULT 3 NOT NULL, -- 最大重试次数
	error_message text NULL, -- 错误信息
	last_error_time timestamp NULL, -- 最后错误时间
	scheduled_time timestamp NOT NULL, -- 计划执行时间
	executed_time timestamp NULL, -- 实际执行时间
	completed_time timestamp NULL, -- 完成时间
	create_by int8 NULL, -- 创建人ID
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 创建时间
	update_by int8 NULL, -- 更新人ID
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 更新时间
	deleted int2 DEFAULT 0 NULL, -- 删除标记：0-未删除，1-已删除
	"version" int4 DEFAULT 0 NULL, -- 版本号（乐观锁）
	CONSTRAINT t_credit_deduction_task_pkey PRIMARY KEY (id),
	CONSTRAINT t_credit_deduction_task_task_id_key UNIQUE (task_id)
);
CREATE INDEX idx_credit_deduction_task_create_time ON public.t_credit_deduction_task USING btree (create_time);
CREATE INDEX idx_credit_deduction_task_resource_usage_record_id ON public.t_credit_deduction_task USING btree (resource_usage_record_id);
CREATE INDEX idx_credit_deduction_task_scheduled_time ON public.t_credit_deduction_task USING btree (scheduled_time);
CREATE INDEX idx_credit_deduction_task_status ON public.t_credit_deduction_task USING btree (status);
CREATE INDEX idx_credit_deduction_task_user_id ON public.t_credit_deduction_task USING btree (user_id);
COMMENT ON TABLE public.t_credit_deduction_task IS '积分扣减任务表';

-- Column comments

COMMENT ON COLUMN public.t_credit_deduction_task.id IS '主键ID';
COMMENT ON COLUMN public.t_credit_deduction_task.task_id IS '任务ID（唯一标识）';
COMMENT ON COLUMN public.t_credit_deduction_task.user_id IS '用户ID';
COMMENT ON COLUMN public.t_credit_deduction_task.amount IS '扣减金额';
COMMENT ON COLUMN public.t_credit_deduction_task.description IS '扣减描述';
COMMENT ON COLUMN public.t_credit_deduction_task.related_order_id IS '关联订单ID';
COMMENT ON COLUMN public.t_credit_deduction_task.resource_usage_record_id IS '关联的资源使用记录ID';
COMMENT ON COLUMN public.t_credit_deduction_task.status IS '任务状态：0-待处理，1-处理中，2-成功，3-失败';
COMMENT ON COLUMN public.t_credit_deduction_task.retry_count IS '重试次数';
COMMENT ON COLUMN public.t_credit_deduction_task.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN public.t_credit_deduction_task.error_message IS '错误信息';
COMMENT ON COLUMN public.t_credit_deduction_task.last_error_time IS '最后错误时间';
COMMENT ON COLUMN public.t_credit_deduction_task.scheduled_time IS '计划执行时间';
COMMENT ON COLUMN public.t_credit_deduction_task.executed_time IS '实际执行时间';
COMMENT ON COLUMN public.t_credit_deduction_task.completed_time IS '完成时间';
COMMENT ON COLUMN public.t_credit_deduction_task.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_credit_deduction_task.create_time IS '创建时间';
COMMENT ON COLUMN public.t_credit_deduction_task.update_by IS '更新人ID';
COMMENT ON COLUMN public.t_credit_deduction_task.update_time IS '更新时间';
COMMENT ON COLUMN public.t_credit_deduction_task.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_credit_deduction_task."version" IS '版本号（乐观锁）';


-- public.t_credit_expiry_log definition

-- Drop table

-- DROP TABLE t_credit_expiry_log;

CREATE TABLE t_credit_expiry_log (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID
	credit_type varchar(32) NOT NULL, -- 积分类型代码
	expired_amount numeric(15, 2) NOT NULL, -- 过期积分数量
	expire_date date NOT NULL, -- 过期日期
	original_transaction_id int8 NULL, -- 原始积分交易记录ID
	processed_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 处理时间
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_credit_expiry_log_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_credit_expiry_log_create_time ON public.t_credit_expiry_log USING btree (create_time);
CREATE INDEX idx_credit_expiry_log_credit_type ON public.t_credit_expiry_log USING btree (credit_type);
CREATE INDEX idx_credit_expiry_log_deleted ON public.t_credit_expiry_log USING btree (deleted);
CREATE INDEX idx_credit_expiry_log_expire_date ON public.t_credit_expiry_log USING btree (expire_date);
CREATE INDEX idx_credit_expiry_log_user_id ON public.t_credit_expiry_log USING btree (user_id);
COMMENT ON TABLE public.t_credit_expiry_log IS '积分过期清理日志表';

-- Column comments

COMMENT ON COLUMN public.t_credit_expiry_log.id IS '主键ID';
COMMENT ON COLUMN public.t_credit_expiry_log.user_id IS '用户ID';
COMMENT ON COLUMN public.t_credit_expiry_log.credit_type IS '积分类型代码';
COMMENT ON COLUMN public.t_credit_expiry_log.expired_amount IS '过期积分数量';
COMMENT ON COLUMN public.t_credit_expiry_log.expire_date IS '过期日期';
COMMENT ON COLUMN public.t_credit_expiry_log.original_transaction_id IS '原始积分交易记录ID';
COMMENT ON COLUMN public.t_credit_expiry_log.processed_time IS '处理时间';
COMMENT ON COLUMN public.t_credit_expiry_log.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_credit_expiry_log.create_time IS '创建时间';
COMMENT ON COLUMN public.t_credit_expiry_log.update_time IS '更新时间';
COMMENT ON COLUMN public.t_credit_expiry_log.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_credit_expiry_log.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_credit_expiry_log_updated_at before
update
    on
    public.t_credit_expiry_log for each row execute function update_updated_at_column();


-- public.t_credit_transaction definition

-- Drop table

-- DROP TABLE t_credit_transaction;

CREATE TABLE t_credit_transaction (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID（逻辑外键->t_user.id）
	transaction_type int2 NOT NULL, -- 交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零
	credit_type varchar(32) NULL, -- 积分类型代码
	amount numeric(15, 2) NOT NULL, -- 交易金额（正数表示收入，负数表示支出）
	balance_before numeric(15, 2) NOT NULL, -- 交易前余额
	balance_after numeric(15, 2) NOT NULL, -- 交易后余额
	description varchar(255) NULL, -- 交易描述
	related_order_id int8 NULL, -- 关联订单ID
	related_subscription_id int8 NULL, -- 关联订阅ID（逻辑外键->t_user_subscription.id）
	expire_time timestamp NULL, -- 过期时间（包月积分）
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_credit_transaction_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_credit_transaction_create_time ON public.t_credit_transaction USING btree (create_time);
CREATE INDEX idx_credit_transaction_credit_type ON public.t_credit_transaction USING btree (credit_type);
CREATE INDEX idx_credit_transaction_deleted ON public.t_credit_transaction USING btree (deleted);
CREATE INDEX idx_credit_transaction_type ON public.t_credit_transaction USING btree (transaction_type);
CREATE INDEX idx_credit_transaction_user_deleted_time ON public.t_credit_transaction USING btree (user_id, deleted, create_time);
CREATE INDEX idx_credit_transaction_user_id ON public.t_credit_transaction USING btree (user_id);
COMMENT ON TABLE public.t_credit_transaction IS '积分交易记录表';

-- Column comments

COMMENT ON COLUMN public.t_credit_transaction.id IS '主键ID';
COMMENT ON COLUMN public.t_credit_transaction.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN public.t_credit_transaction.transaction_type IS '交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零';
COMMENT ON COLUMN public.t_credit_transaction.credit_type IS '积分类型代码';
COMMENT ON COLUMN public.t_credit_transaction.amount IS '交易金额（正数表示收入，负数表示支出）';
COMMENT ON COLUMN public.t_credit_transaction.balance_before IS '交易前余额';
COMMENT ON COLUMN public.t_credit_transaction.balance_after IS '交易后余额';
COMMENT ON COLUMN public.t_credit_transaction.description IS '交易描述';
COMMENT ON COLUMN public.t_credit_transaction.related_order_id IS '关联订单ID';
COMMENT ON COLUMN public.t_credit_transaction.related_subscription_id IS '关联订阅ID（逻辑外键->t_user_subscription.id）';
COMMENT ON COLUMN public.t_credit_transaction.expire_time IS '过期时间（包月积分）';
COMMENT ON COLUMN public.t_credit_transaction.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_credit_transaction.create_time IS '创建时间';
COMMENT ON COLUMN public.t_credit_transaction.update_time IS '更新时间';
COMMENT ON COLUMN public.t_credit_transaction.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_credit_transaction.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_credit_transaction_updated_at before
update
    on
    public.t_credit_transaction for each row execute function update_updated_at_column();


-- public.t_credit_type_config definition

-- Drop table

-- DROP TABLE t_credit_type_config;

CREATE TABLE t_credit_type_config (
	id bigserial NOT NULL, -- 主键ID
	type_code varchar(32) NOT NULL, -- 积分类型代码
	type_name varchar(64) NOT NULL, -- 积分类型名称
	validity_days int4 DEFAULT 0 NOT NULL, -- 有效期天数，0表示永久
	consume_priority int4 NOT NULL, -- 消费优先级，数字越小优先级越高
	description varchar(200) NULL, -- 描述
	enabled int2 DEFAULT 1 NOT NULL, -- 是否启用
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_credit_type_config_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_credit_type_config_consume_priority ON public.t_credit_type_config USING btree (consume_priority);
CREATE INDEX idx_credit_type_config_create_time ON public.t_credit_type_config USING btree (create_time);
CREATE INDEX idx_credit_type_config_deleted ON public.t_credit_type_config USING btree (deleted);
CREATE INDEX idx_credit_type_config_enabled ON public.t_credit_type_config USING btree (enabled);
CREATE UNIQUE INDEX uk_credit_type_config_type_code ON public.t_credit_type_config USING btree (type_code);
COMMENT ON TABLE public.t_credit_type_config IS '积分类型配置表';

-- Column comments

COMMENT ON COLUMN public.t_credit_type_config.id IS '主键ID';
COMMENT ON COLUMN public.t_credit_type_config.type_code IS '积分类型代码';
COMMENT ON COLUMN public.t_credit_type_config.type_name IS '积分类型名称';
COMMENT ON COLUMN public.t_credit_type_config.validity_days IS '有效期天数，0表示永久';
COMMENT ON COLUMN public.t_credit_type_config.consume_priority IS '消费优先级，数字越小优先级越高';
COMMENT ON COLUMN public.t_credit_type_config.description IS '描述';
COMMENT ON COLUMN public.t_credit_type_config.enabled IS '是否启用';
COMMENT ON COLUMN public.t_credit_type_config.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_credit_type_config.create_time IS '创建时间';
COMMENT ON COLUMN public.t_credit_type_config.update_time IS '更新时间';
COMMENT ON COLUMN public.t_credit_type_config.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_credit_type_config.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_credit_type_config_updated_at before
update
    on
    public.t_credit_type_config for each row execute function update_updated_at_column();


-- public.t_daily_credit_grant_log definition

-- Drop table

-- DROP TABLE t_daily_credit_grant_log;

CREATE TABLE t_daily_credit_grant_log (
	id bigserial NOT NULL, -- 主键ID
	grant_date date NOT NULL, -- 发放日期
	total_users int4 DEFAULT 0 NOT NULL, -- 总用户数
	success_users int4 DEFAULT 0 NOT NULL, -- 成功发放用户数
	failed_users int4 DEFAULT 0 NOT NULL, -- 失败用户数
	skipped_users int4 DEFAULT 0 NOT NULL, -- 跳过用户数（已发放过）
	total_credits numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 总发放积分
	execution_duration_ms int8 NULL, -- 执行耗时（毫秒）
	start_time timestamp NULL, -- 开始时间
	end_time timestamp NULL, -- 结束时间
	result_message text NULL, -- 执行结果详情
	created_by varchar(50) DEFAULT 'SYSTEM'::character varying NOT NULL, -- 创建者
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	CONSTRAINT t_daily_credit_grant_log_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_daily_credit_grant_log_create_time ON public.t_daily_credit_grant_log USING btree (create_time);
CREATE UNIQUE INDEX uk_daily_credit_grant_log_grant_date ON public.t_daily_credit_grant_log USING btree (grant_date);
COMMENT ON TABLE public.t_daily_credit_grant_log IS '每日积分发放日志表';

-- Column comments

COMMENT ON COLUMN public.t_daily_credit_grant_log.id IS '主键ID';
COMMENT ON COLUMN public.t_daily_credit_grant_log.grant_date IS '发放日期';
COMMENT ON COLUMN public.t_daily_credit_grant_log.total_users IS '总用户数';
COMMENT ON COLUMN public.t_daily_credit_grant_log.success_users IS '成功发放用户数';
COMMENT ON COLUMN public.t_daily_credit_grant_log.failed_users IS '失败用户数';
COMMENT ON COLUMN public.t_daily_credit_grant_log.skipped_users IS '跳过用户数（已发放过）';
COMMENT ON COLUMN public.t_daily_credit_grant_log.total_credits IS '总发放积分';
COMMENT ON COLUMN public.t_daily_credit_grant_log.execution_duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN public.t_daily_credit_grant_log.start_time IS '开始时间';
COMMENT ON COLUMN public.t_daily_credit_grant_log.end_time IS '结束时间';
COMMENT ON COLUMN public.t_daily_credit_grant_log.result_message IS '执行结果详情';
COMMENT ON COLUMN public.t_daily_credit_grant_log.created_by IS '创建者';
COMMENT ON COLUMN public.t_daily_credit_grant_log.create_time IS '创建时间';


-- public.t_payment_record definition

-- Drop table

-- DROP TABLE t_payment_record;

CREATE TABLE t_payment_record (
	id bigserial NOT NULL, -- 主键ID
	order_id int8 NOT NULL, -- 订单ID
	order_no varchar(64) NOT NULL, -- 订单号
	user_id int8 NOT NULL, -- 用户ID
	amount numeric(10, 2) NOT NULL, -- 支付金额
	payment_method varchar(20) NOT NULL, -- 支付方式
	third_party_order_no varchar(100) NULL, -- 第三方支付订单号
	third_party_transaction_no varchar(100) NULL, -- 第三方交易流水号
	status varchar(20) DEFAULT 'pending'::character varying NOT NULL, -- 支付状态
	qr_code text NULL, -- 支付二维码
	payment_url varchar(500) NULL, -- 支付链接
	paid_at timestamp NULL, -- 支付时间
	callback_data text NULL, -- 第三方回调数据
	failure_reason varchar(500) NULL, -- 失败原因
	remark varchar(500) NULL, -- 备注
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_payment_record_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_payment_record_create_time ON public.t_payment_record USING btree (create_time);
CREATE INDEX idx_payment_record_deleted ON public.t_payment_record USING btree (deleted);
CREATE INDEX idx_payment_record_order_id ON public.t_payment_record USING btree (order_id);
CREATE INDEX idx_payment_record_order_no ON public.t_payment_record USING btree (order_no);
CREATE INDEX idx_payment_record_payment_method ON public.t_payment_record USING btree (payment_method);
CREATE INDEX idx_payment_record_status ON public.t_payment_record USING btree (status);
CREATE INDEX idx_payment_record_third_party_order_no ON public.t_payment_record USING btree (third_party_order_no);
CREATE INDEX idx_payment_record_user_id ON public.t_payment_record USING btree (user_id);
COMMENT ON TABLE public.t_payment_record IS '支付记录表';

-- Column comments

COMMENT ON COLUMN public.t_payment_record.id IS '主键ID';
COMMENT ON COLUMN public.t_payment_record.order_id IS '订单ID';
COMMENT ON COLUMN public.t_payment_record.order_no IS '订单号';
COMMENT ON COLUMN public.t_payment_record.user_id IS '用户ID';
COMMENT ON COLUMN public.t_payment_record.amount IS '支付金额';
COMMENT ON COLUMN public.t_payment_record.payment_method IS '支付方式';
COMMENT ON COLUMN public.t_payment_record.third_party_order_no IS '第三方支付订单号';
COMMENT ON COLUMN public.t_payment_record.third_party_transaction_no IS '第三方交易流水号';
COMMENT ON COLUMN public.t_payment_record.status IS '支付状态';
COMMENT ON COLUMN public.t_payment_record.qr_code IS '支付二维码';
COMMENT ON COLUMN public.t_payment_record.payment_url IS '支付链接';
COMMENT ON COLUMN public.t_payment_record.paid_at IS '支付时间';
COMMENT ON COLUMN public.t_payment_record.callback_data IS '第三方回调数据';
COMMENT ON COLUMN public.t_payment_record.failure_reason IS '失败原因';
COMMENT ON COLUMN public.t_payment_record.remark IS '备注';
COMMENT ON COLUMN public.t_payment_record.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_payment_record.create_time IS '创建时间';
COMMENT ON COLUMN public.t_payment_record.update_time IS '更新时间';
COMMENT ON COLUMN public.t_payment_record.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_payment_record.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_payment_record_updated_at before
update
    on
    public.t_payment_record for each row execute function update_updated_at_column();


-- public.t_resource_usage_record definition

-- Drop table

-- DROP TABLE t_resource_usage_record;

CREATE TABLE t_resource_usage_record (
	id bigserial NOT NULL, -- 主键ID
	request_id varchar(100) NOT NULL, -- 请求ID（幂等键）
	report_id varchar(50) NOT NULL, -- 报告ID
	user_id int8 NOT NULL, -- 用户ID（雪花算法生成）
	agent_id varchar(100) NOT NULL, -- 智能体ID
	context_id varchar(100) NULL, -- 会话ID
	task_type varchar(50) NOT NULL, -- 任务类型：INDUSTRY_RESEARCH_REPORT, PPT_GENERATION等
	task_description varchar(500) NULL, -- 任务描述
	resource_type varchar(20) NOT NULL, -- 资源类型：MODEL, FUNCTION, MEDIA
	resource_name varchar(100) NOT NULL, -- 资源名称：模型名称、功能类型、媒体类型等
	resource_subtype varchar(50) NULL, -- 资源子类型：TEXT_GENERATION, IMAGE_GENERATION等
	usage_data text NULL, -- 使用量JSON数据（Token数、次数、秒数等）
	billing_unit varchar(20) NULL, -- 计费单位：TOKEN, TIMES, PAGES, COUNT, SECONDS
	usage_amount numeric(15, 6) DEFAULT 0 NULL, -- 使用量
	unit_price numeric(10, 6) DEFAULT 0 NULL, -- 单价（元）
	billing_amount numeric(15, 6) DEFAULT 0 NULL, -- 计费金额（元）
	description varchar(500) NULL, -- 描述
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	deleted int2 DEFAULT 0 NULL, -- 删除标记：0-未删除，1-已删除
	CONSTRAINT t_resource_usage_record_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_resource_usage_agent_id ON public.t_resource_usage_record USING btree (agent_id);
CREATE INDEX idx_resource_usage_context_id ON public.t_resource_usage_record USING btree (context_id);
CREATE INDEX idx_resource_usage_create_time ON public.t_resource_usage_record USING btree (create_time);
CREATE INDEX idx_resource_usage_report_id ON public.t_resource_usage_record USING btree (report_id);
CREATE INDEX idx_resource_usage_request_id ON public.t_resource_usage_record USING btree (request_id);
CREATE INDEX idx_resource_usage_resource_type ON public.t_resource_usage_record USING btree (resource_type);
CREATE INDEX idx_resource_usage_task_resource ON public.t_resource_usage_record USING btree (task_type, resource_type);
CREATE INDEX idx_resource_usage_task_type ON public.t_resource_usage_record USING btree (task_type);
CREATE INDEX idx_resource_usage_user_id ON public.t_resource_usage_record USING btree (user_id);
CREATE INDEX idx_resource_usage_user_time ON public.t_resource_usage_record USING btree (user_id, create_time);
COMMENT ON TABLE public.t_resource_usage_record IS '资源使用记录表';

-- Column comments

COMMENT ON COLUMN public.t_resource_usage_record.id IS '主键ID';
COMMENT ON COLUMN public.t_resource_usage_record.request_id IS '请求ID（幂等键）';
COMMENT ON COLUMN public.t_resource_usage_record.report_id IS '报告ID';
COMMENT ON COLUMN public.t_resource_usage_record.user_id IS '用户ID（雪花算法生成）';
COMMENT ON COLUMN public.t_resource_usage_record.agent_id IS '智能体ID';
COMMENT ON COLUMN public.t_resource_usage_record.context_id IS '会话ID';
COMMENT ON COLUMN public.t_resource_usage_record.task_type IS '任务类型：INDUSTRY_RESEARCH_REPORT, PPT_GENERATION等';
COMMENT ON COLUMN public.t_resource_usage_record.task_description IS '任务描述';
COMMENT ON COLUMN public.t_resource_usage_record.resource_type IS '资源类型：MODEL, FUNCTION, MEDIA';
COMMENT ON COLUMN public.t_resource_usage_record.resource_name IS '资源名称：模型名称、功能类型、媒体类型等';
COMMENT ON COLUMN public.t_resource_usage_record.resource_subtype IS '资源子类型：TEXT_GENERATION, IMAGE_GENERATION等';
COMMENT ON COLUMN public.t_resource_usage_record.usage_data IS '使用量JSON数据（Token数、次数、秒数等）';
COMMENT ON COLUMN public.t_resource_usage_record.billing_unit IS '计费单位：TOKEN, TIMES, PAGES, COUNT, SECONDS';
COMMENT ON COLUMN public.t_resource_usage_record.usage_amount IS '使用量';
COMMENT ON COLUMN public.t_resource_usage_record.unit_price IS '单价（元）';
COMMENT ON COLUMN public.t_resource_usage_record.billing_amount IS '计费金额（元）';
COMMENT ON COLUMN public.t_resource_usage_record.description IS '描述';
COMMENT ON COLUMN public.t_resource_usage_record.create_time IS '创建时间';
COMMENT ON COLUMN public.t_resource_usage_record.update_time IS '更新时间';
COMMENT ON COLUMN public.t_resource_usage_record.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_resource_usage_record.update_by IS '更新人ID';
COMMENT ON COLUMN public.t_resource_usage_record.deleted IS '删除标记：0-未删除，1-已删除';

-- Table Triggers

create trigger update_t_resource_usage_record_updated_at before
update
    on
    public.t_resource_usage_record for each row execute function update_updated_at_column();


-- public.t_scheduled_chat_task definition

-- Drop table

-- DROP TABLE t_scheduled_chat_task;

CREATE TABLE t_scheduled_chat_task (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID
	workspace_id int8 NOT NULL, -- 工作空间ID
	chat_task_id int8 NULL, -- 对话任务ID
	task_name varchar(100) NOT NULL, -- 任务名称
	cron_expression varchar(50) NOT NULL, -- Cron表达式
	prompt text NOT NULL, -- 对话提示词
	status int2 DEFAULT 1 NOT NULL, -- 状态: 1启用 0禁用
	last_execution_time timestamp NULL, -- 上次执行时间
	next_execution_time timestamp NULL, -- 下次执行时间
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	task_type int2 DEFAULT 1 NULL,
	schedule_config text NULL, -- 任务调度配置（JSON格式存储）
	CONSTRAINT t_scheduled_chat_task_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_scheduled_chat_task_chat_task_id ON public.t_scheduled_chat_task USING btree (chat_task_id);
CREATE INDEX idx_scheduled_chat_task_deleted ON public.t_scheduled_chat_task USING btree (deleted);
CREATE INDEX idx_scheduled_chat_task_next_execution_time ON public.t_scheduled_chat_task USING btree (next_execution_time);
CREATE INDEX idx_scheduled_chat_task_schedule_config ON public.t_scheduled_chat_task USING btree (schedule_config);
CREATE INDEX idx_scheduled_chat_task_status ON public.t_scheduled_chat_task USING btree (status);
CREATE INDEX idx_scheduled_chat_task_user_deleted_status ON public.t_scheduled_chat_task USING btree (user_id, deleted, status);
CREATE INDEX idx_scheduled_chat_task_user_id ON public.t_scheduled_chat_task USING btree (user_id);
CREATE INDEX idx_scheduled_chat_task_workspace_id ON public.t_scheduled_chat_task USING btree (workspace_id);
COMMENT ON TABLE public.t_scheduled_chat_task IS '定时对话任务表';

-- Column comments

COMMENT ON COLUMN public.t_scheduled_chat_task.id IS '主键ID';
COMMENT ON COLUMN public.t_scheduled_chat_task.user_id IS '用户ID';
COMMENT ON COLUMN public.t_scheduled_chat_task.workspace_id IS '工作空间ID';
COMMENT ON COLUMN public.t_scheduled_chat_task.chat_task_id IS '对话任务ID';
COMMENT ON COLUMN public.t_scheduled_chat_task.task_name IS '任务名称';
COMMENT ON COLUMN public.t_scheduled_chat_task.cron_expression IS 'Cron表达式';
COMMENT ON COLUMN public.t_scheduled_chat_task.prompt IS '对话提示词';
COMMENT ON COLUMN public.t_scheduled_chat_task.status IS '状态: 1启用 0禁用';
COMMENT ON COLUMN public.t_scheduled_chat_task.last_execution_time IS '上次执行时间';
COMMENT ON COLUMN public.t_scheduled_chat_task.next_execution_time IS '下次执行时间';
COMMENT ON COLUMN public.t_scheduled_chat_task.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_scheduled_chat_task.create_time IS '创建时间';
COMMENT ON COLUMN public.t_scheduled_chat_task.update_time IS '更新时间';
COMMENT ON COLUMN public.t_scheduled_chat_task.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_scheduled_chat_task.update_by IS '更新人ID';
COMMENT ON COLUMN public.t_scheduled_chat_task.schedule_config IS '任务调度配置（JSON格式存储）';

-- Table Triggers

create trigger update_t_scheduled_chat_task_updated_at before
update
    on
    public.t_scheduled_chat_task for each row execute function update_updated_at_column();


-- public.t_scheduled_chat_task_execution_log definition

-- Drop table

-- DROP TABLE t_scheduled_chat_task_execution_log;

CREATE TABLE t_scheduled_chat_task_execution_log (
	id bigserial NOT NULL, -- 主键ID
	task_id int8 NOT NULL, -- 定时任务ID
	chat_task_id int8 NULL, -- 对话任务ID
	task_name varchar(100) NOT NULL, -- 任务名称
	start_time timestamp NOT NULL, -- 执行开始时间
	end_time timestamp NULL, -- 执行结束时间
	execution_status int2 NOT NULL, -- 执行状态: 1成功 0失败
	execution_result text NULL, -- 执行结果
	duration int8 NULL, -- 执行耗时(毫秒)
	error_message text NULL, -- 错误信息
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	CONSTRAINT t_scheduled_chat_task_execution_log_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_scheduled_chat_task_execution_log_chat_task_id ON public.t_scheduled_chat_task_execution_log USING btree (chat_task_id);
CREATE INDEX idx_scheduled_chat_task_execution_log_execution_status ON public.t_scheduled_chat_task_execution_log USING btree (execution_status);
CREATE INDEX idx_scheduled_chat_task_execution_log_start_time ON public.t_scheduled_chat_task_execution_log USING btree (start_time);
CREATE INDEX idx_scheduled_chat_task_execution_log_task_id ON public.t_scheduled_chat_task_execution_log USING btree (task_id);
CREATE INDEX idx_scheduled_chat_task_execution_log_task_name ON public.t_scheduled_chat_task_execution_log USING btree (task_name);
COMMENT ON TABLE public.t_scheduled_chat_task_execution_log IS '定时对话任务执行日志表';

-- Column comments

COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.id IS '主键ID';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.task_id IS '定时任务ID';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.chat_task_id IS '对话任务ID';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.task_name IS '任务名称';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.start_time IS '执行开始时间';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.end_time IS '执行结束时间';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.execution_status IS '执行状态: 1成功 0失败';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.execution_result IS '执行结果';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.duration IS '执行耗时(毫秒)';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.error_message IS '错误信息';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.create_time IS '创建时间';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.update_time IS '更新时间';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.update_by IS '更新人ID';
COMMENT ON COLUMN public.t_scheduled_chat_task_execution_log.deleted IS '删除标记：0-未删除，1-已删除';

-- Table Triggers

create trigger update_t_scheduled_chat_task_execution_log_updated_at before
update
    on
    public.t_scheduled_chat_task_execution_log for each row execute function update_updated_at_column();


-- public.t_scheduled_execution_logs definition

-- Drop table

-- DROP TABLE t_scheduled_execution_logs;

CREATE TABLE t_scheduled_execution_logs (
	id bigserial NOT NULL, -- 主键ID
	task_name text NOT NULL, -- 任务名称
	task_instance text NOT NULL, -- 任务实例ID
	task_data bytea NULL, -- 任务数据
	picked_by text NULL, -- 执行者标识
	time_started timestamptz NOT NULL, -- 任务开始执行时间
	time_finished timestamptz NOT NULL, -- 任务完成时间
	succeeded bool NOT NULL, -- 任务是否执行成功
	duration_ms int8 NOT NULL, -- 任务执行耗时（毫秒）
	exception_class text NULL, -- 异常类名
	exception_message text NULL, -- 异常消息
	exception_stacktrace text NULL, -- 异常堆栈信息
	CONSTRAINT t_scheduled_execution_logs_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_t_scheduled_execution_logs_exception_class ON public.t_scheduled_execution_logs USING btree (exception_class);
CREATE INDEX idx_t_scheduled_execution_logs_started ON public.t_scheduled_execution_logs USING btree (time_started);
CREATE INDEX idx_t_scheduled_execution_logs_task_name ON public.t_scheduled_execution_logs USING btree (task_name);
COMMENT ON TABLE public.t_scheduled_execution_logs IS 'db-scheduler 任务执行历史记录表（标准格式）';

-- Column comments

COMMENT ON COLUMN public.t_scheduled_execution_logs.id IS '主键ID';
COMMENT ON COLUMN public.t_scheduled_execution_logs.task_name IS '任务名称';
COMMENT ON COLUMN public.t_scheduled_execution_logs.task_instance IS '任务实例ID';
COMMENT ON COLUMN public.t_scheduled_execution_logs.task_data IS '任务数据';
COMMENT ON COLUMN public.t_scheduled_execution_logs.picked_by IS '执行者标识';
COMMENT ON COLUMN public.t_scheduled_execution_logs.time_started IS '任务开始执行时间';
COMMENT ON COLUMN public.t_scheduled_execution_logs.time_finished IS '任务完成时间';
COMMENT ON COLUMN public.t_scheduled_execution_logs.succeeded IS '任务是否执行成功';
COMMENT ON COLUMN public.t_scheduled_execution_logs.duration_ms IS '任务执行耗时（毫秒）';
COMMENT ON COLUMN public.t_scheduled_execution_logs.exception_class IS '异常类名';
COMMENT ON COLUMN public.t_scheduled_execution_logs.exception_message IS '异常消息';
COMMENT ON COLUMN public.t_scheduled_execution_logs.exception_stacktrace IS '异常堆栈信息';


-- public.t_scheduled_task definition

-- Drop table

-- DROP TABLE t_scheduled_task;

CREATE TABLE t_scheduled_task (
	task_name varchar(128) NOT NULL, -- 任务名称（扩展到128字符）
	task_instance varchar(256) NOT NULL, -- 任务实例ID（256字符支持长订单号）
	task_data bytea NULL, -- 任务数据（二进制格式）
	execution_time timestamp(6) NOT NULL, -- 计划执行时间
	picked bool NOT NULL, -- 是否被选中执行
	picked_by varchar(50) NULL, -- 执行者标识
	last_success timestamp(6) NULL, -- 最后成功执行时间
	last_failure timestamp(6) NULL, -- 最后失败时间
	consecutive_failures int4 NULL, -- 连续失败次数
	last_heartbeat timestamp(6) NULL, -- 最后心跳时间
	"version" int8 NOT NULL, -- 版本号（用于乐观锁）
	CONSTRAINT t_scheduled_task_pkey PRIMARY KEY (task_name, task_instance)
);
CREATE INDEX execution_time_idx ON public.t_scheduled_task USING btree (execution_time);
CREATE INDEX idx_scheduled_task_execution_time ON public.t_scheduled_task USING btree (execution_time);
CREATE INDEX idx_scheduled_task_last_heartbeat ON public.t_scheduled_task USING btree (last_heartbeat);
CREATE INDEX idx_scheduled_task_picked ON public.t_scheduled_task USING btree (picked);
CREATE INDEX idx_scheduled_task_picked_heartbeat ON public.t_scheduled_task USING btree (picked, last_heartbeat);
CREATE INDEX last_heartbeat_idx ON public.t_scheduled_task USING btree (last_heartbeat);
COMMENT ON TABLE public.t_scheduled_task IS '定时任务调度表 - db-scheduler主表';

-- Column comments

COMMENT ON COLUMN public.t_scheduled_task.task_name IS '任务名称（扩展到128字符）';
COMMENT ON COLUMN public.t_scheduled_task.task_instance IS '任务实例ID（256字符支持长订单号）';
COMMENT ON COLUMN public.t_scheduled_task.task_data IS '任务数据（二进制格式）';
COMMENT ON COLUMN public.t_scheduled_task.execution_time IS '计划执行时间';
COMMENT ON COLUMN public.t_scheduled_task.picked IS '是否被选中执行';
COMMENT ON COLUMN public.t_scheduled_task.picked_by IS '执行者标识';
COMMENT ON COLUMN public.t_scheduled_task.last_success IS '最后成功执行时间';
COMMENT ON COLUMN public.t_scheduled_task.last_failure IS '最后失败时间';
COMMENT ON COLUMN public.t_scheduled_task.consecutive_failures IS '连续失败次数';
COMMENT ON COLUMN public.t_scheduled_task.last_heartbeat IS '最后心跳时间';
COMMENT ON COLUMN public.t_scheduled_task."version" IS '版本号（用于乐观锁）';


-- public.t_subscription_order definition

-- Drop table

-- DROP TABLE t_subscription_order;

CREATE TABLE t_subscription_order (
	id bigserial NOT NULL, -- 主键ID
	order_no varchar(64) NOT NULL, -- 订单号
	user_id int8 NOT NULL, -- 用户ID
	plan_id int8 NOT NULL, -- 套餐ID
	plan_name varchar(100) NOT NULL, -- 套餐名称
	amount numeric(10, 2) NOT NULL, -- 订单金额
	billing_cycle varchar(20) DEFAULT 'monthly'::character varying NOT NULL, -- 计费周期
	status varchar(20) DEFAULT 'pending'::character varying NOT NULL, -- 订单状态
	payment_method varchar(20) NULL, -- 支付方式
	third_party_order_no varchar(100) NULL, -- 第三方支付订单号
	paid_at timestamp NULL, -- 支付时间
	expired_at timestamp NOT NULL, -- 过期时间
	effective_start_time timestamp NULL, -- 套餐生效开始时间
	effective_end_time timestamp NULL, -- 套餐生效结束时间
	remark varchar(500) NULL, -- 备注
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_subscription_order_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_subscription_order_create_time ON public.t_subscription_order USING btree (create_time);
CREATE INDEX idx_subscription_order_deleted ON public.t_subscription_order USING btree (deleted);
CREATE INDEX idx_subscription_order_plan_id ON public.t_subscription_order USING btree (plan_id);
CREATE INDEX idx_subscription_order_status ON public.t_subscription_order USING btree (status);
CREATE INDEX idx_subscription_order_user_id ON public.t_subscription_order USING btree (user_id);
CREATE UNIQUE INDEX uk_subscription_order_order_no ON public.t_subscription_order USING btree (order_no);
COMMENT ON TABLE public.t_subscription_order IS '订阅订单表';

-- Column comments

COMMENT ON COLUMN public.t_subscription_order.id IS '主键ID';
COMMENT ON COLUMN public.t_subscription_order.order_no IS '订单号';
COMMENT ON COLUMN public.t_subscription_order.user_id IS '用户ID';
COMMENT ON COLUMN public.t_subscription_order.plan_id IS '套餐ID';
COMMENT ON COLUMN public.t_subscription_order.plan_name IS '套餐名称';
COMMENT ON COLUMN public.t_subscription_order.amount IS '订单金额';
COMMENT ON COLUMN public.t_subscription_order.billing_cycle IS '计费周期';
COMMENT ON COLUMN public.t_subscription_order.status IS '订单状态';
COMMENT ON COLUMN public.t_subscription_order.payment_method IS '支付方式';
COMMENT ON COLUMN public.t_subscription_order.third_party_order_no IS '第三方支付订单号';
COMMENT ON COLUMN public.t_subscription_order.paid_at IS '支付时间';
COMMENT ON COLUMN public.t_subscription_order.expired_at IS '过期时间';
COMMENT ON COLUMN public.t_subscription_order.effective_start_time IS '套餐生效开始时间';
COMMENT ON COLUMN public.t_subscription_order.effective_end_time IS '套餐生效结束时间';
COMMENT ON COLUMN public.t_subscription_order.remark IS '备注';
COMMENT ON COLUMN public.t_subscription_order.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_subscription_order.create_time IS '创建时间';
COMMENT ON COLUMN public.t_subscription_order.update_time IS '更新时间';
COMMENT ON COLUMN public.t_subscription_order.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_subscription_order.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_subscription_order_updated_at before
update
    on
    public.t_subscription_order for each row execute function update_updated_at_column();


-- public.t_user_credit_balance definition

-- Drop table

-- DROP TABLE t_user_credit_balance;

CREATE TABLE t_user_credit_balance (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID
	credit_type varchar(32) NOT NULL, -- 积分类型代码
	balance numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 余额
	total_earned numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 累计获得
	total_spent numeric(15, 2) DEFAULT 0.00 NOT NULL, -- 累计消费
	last_earn_time timestamp NULL, -- 最后获得时间
	last_spend_time timestamp NULL, -- 最后消费时间
	"version" int4 DEFAULT 0 NOT NULL, -- 版本号（乐观锁）
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_user_credit_balance_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_user_credit_balance_balance ON public.t_user_credit_balance USING btree (balance);
CREATE INDEX idx_user_credit_balance_create_time ON public.t_user_credit_balance USING btree (create_time);
CREATE INDEX idx_user_credit_balance_credit_type ON public.t_user_credit_balance USING btree (credit_type);
CREATE INDEX idx_user_credit_balance_deleted ON public.t_user_credit_balance USING btree (deleted);
CREATE INDEX idx_user_credit_balance_user_id ON public.t_user_credit_balance USING btree (user_id);
CREATE UNIQUE INDEX uk_user_credit_balance_user_credit_type ON public.t_user_credit_balance USING btree (user_id, credit_type, deleted);
COMMENT ON TABLE public.t_user_credit_balance IS '用户积分余额明细表';

-- Column comments

COMMENT ON COLUMN public.t_user_credit_balance.id IS '主键ID';
COMMENT ON COLUMN public.t_user_credit_balance.user_id IS '用户ID';
COMMENT ON COLUMN public.t_user_credit_balance.credit_type IS '积分类型代码';
COMMENT ON COLUMN public.t_user_credit_balance.balance IS '余额';
COMMENT ON COLUMN public.t_user_credit_balance.total_earned IS '累计获得';
COMMENT ON COLUMN public.t_user_credit_balance.total_spent IS '累计消费';
COMMENT ON COLUMN public.t_user_credit_balance.last_earn_time IS '最后获得时间';
COMMENT ON COLUMN public.t_user_credit_balance.last_spend_time IS '最后消费时间';
COMMENT ON COLUMN public.t_user_credit_balance."version" IS '版本号（乐观锁）';
COMMENT ON COLUMN public.t_user_credit_balance.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_user_credit_balance.create_time IS '创建时间';
COMMENT ON COLUMN public.t_user_credit_balance.update_time IS '更新时间';
COMMENT ON COLUMN public.t_user_credit_balance.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_user_credit_balance.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_user_credit_balance_updated_at before
update
    on
    public.t_user_credit_balance for each row execute function update_updated_at_column();


-- public.t_user_daily_login definition

-- Drop table

-- DROP TABLE t_user_daily_login;

CREATE TABLE t_user_daily_login (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID
	login_date date NOT NULL, -- 登录日期
	login_count int4 DEFAULT 1 NULL, -- 当日登录次数
	first_login_time timestamp NOT NULL, -- 当日首次登录时间
	daily_credits_granted bool DEFAULT false NULL, -- 当日积分是否已发放
	daily_credits_amount numeric(10, 2) DEFAULT 0 NULL, -- 当日发放的积分数量
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	deleted int2 DEFAULT 0 NULL, -- 删除标记：0-未删除，1-已删除
	CONSTRAINT t_user_daily_login_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_user_daily_login_create_time ON public.t_user_daily_login USING btree (create_time);
CREATE INDEX idx_user_daily_login_credits_granted ON public.t_user_daily_login USING btree (daily_credits_granted);
CREATE INDEX idx_user_daily_login_date ON public.t_user_daily_login USING btree (login_date);
CREATE INDEX idx_user_daily_login_user_id ON public.t_user_daily_login USING btree (user_id);
CREATE UNIQUE INDEX uk_user_daily_login_user_date ON public.t_user_daily_login USING btree (user_id, login_date) WHERE (deleted = 0);
COMMENT ON TABLE public.t_user_daily_login IS '用户每日登录记录表';

-- Column comments

COMMENT ON COLUMN public.t_user_daily_login.id IS '主键ID';
COMMENT ON COLUMN public.t_user_daily_login.user_id IS '用户ID';
COMMENT ON COLUMN public.t_user_daily_login.login_date IS '登录日期';
COMMENT ON COLUMN public.t_user_daily_login.login_count IS '当日登录次数';
COMMENT ON COLUMN public.t_user_daily_login.first_login_time IS '当日首次登录时间';
COMMENT ON COLUMN public.t_user_daily_login.daily_credits_granted IS '当日积分是否已发放';
COMMENT ON COLUMN public.t_user_daily_login.daily_credits_amount IS '当日发放的积分数量';
COMMENT ON COLUMN public.t_user_daily_login.create_time IS '创建时间';
COMMENT ON COLUMN public.t_user_daily_login.update_time IS '更新时间';
COMMENT ON COLUMN public.t_user_daily_login.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_user_daily_login.update_by IS '更新人ID';
COMMENT ON COLUMN public.t_user_daily_login.deleted IS '删除标记：0-未删除，1-已删除';

-- Table Triggers

create trigger update_t_user_daily_login_updated_at before
update
    on
    public.t_user_daily_login for each row execute function update_updated_at_column();


-- public.t_user_subscription definition

-- Drop table

-- DROP TABLE t_user_subscription;

CREATE TABLE t_user_subscription (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID（逻辑外键->t_user.id）
	plan_id int8 NOT NULL, -- 套餐ID（逻辑外键->t_subscription_plan.id）
	start_time timestamp NOT NULL, -- 订阅开始时间
	end_time timestamp NOT NULL, -- 订阅结束时间
	paid_amount numeric(10, 2) NOT NULL, -- 支付金额
	credit_amount numeric(15, 2) NOT NULL, -- 获得积分数量
	status int2 DEFAULT 1 NOT NULL, -- 订阅状态 1-生效中 2-已过期 3-已取消
	pay_order_no varchar(64) NULL, -- 支付订单号
	remark varchar(255) NULL, -- 备注
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_user_subscription_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_user_subscription_create_time ON public.t_user_subscription USING btree (create_time);
CREATE INDEX idx_user_subscription_deleted ON public.t_user_subscription USING btree (deleted);
CREATE INDEX idx_user_subscription_plan_id ON public.t_user_subscription USING btree (plan_id);
CREATE INDEX idx_user_subscription_status_end_time ON public.t_user_subscription USING btree (status, end_time);
CREATE INDEX idx_user_subscription_user_deleted_status ON public.t_user_subscription USING btree (user_id, deleted, status);
CREATE INDEX idx_user_subscription_user_id ON public.t_user_subscription USING btree (user_id);
COMMENT ON TABLE public.t_user_subscription IS '用户订阅记录表';

-- Column comments

COMMENT ON COLUMN public.t_user_subscription.id IS '主键ID';
COMMENT ON COLUMN public.t_user_subscription.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN public.t_user_subscription.plan_id IS '套餐ID（逻辑外键->t_subscription_plan.id）';
COMMENT ON COLUMN public.t_user_subscription.start_time IS '订阅开始时间';
COMMENT ON COLUMN public.t_user_subscription.end_time IS '订阅结束时间';
COMMENT ON COLUMN public.t_user_subscription.paid_amount IS '支付金额';
COMMENT ON COLUMN public.t_user_subscription.credit_amount IS '获得积分数量';
COMMENT ON COLUMN public.t_user_subscription.status IS '订阅状态 1-生效中 2-已过期 3-已取消';
COMMENT ON COLUMN public.t_user_subscription.pay_order_no IS '支付订单号';
COMMENT ON COLUMN public.t_user_subscription.remark IS '备注';
COMMENT ON COLUMN public.t_user_subscription.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_user_subscription.create_time IS '创建时间';
COMMENT ON COLUMN public.t_user_subscription.update_time IS '更新时间';
COMMENT ON COLUMN public.t_user_subscription.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_user_subscription.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_user_subscription_updated_at before
update
    on
    public.t_user_subscription for each row execute function update_updated_at_column();


-- public.t_user_workspace definition

-- Drop table

-- DROP TABLE t_user_workspace;

CREATE TABLE t_user_workspace (
	id bigserial NOT NULL, -- 主键ID
	user_id int8 NOT NULL, -- 用户ID（逻辑外键->t_user.id）
	"name" varchar(100) NOT NULL, -- 工作空间名称
	description text NULL, -- 工作空间描述
	is_default int2 DEFAULT 0 NOT NULL, -- 是否默认工作空间：1是 0否
	status int2 DEFAULT 1 NOT NULL, -- 状态: 1正常 2禁用
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_user_workspace_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_user_workspace_create_time ON public.t_user_workspace USING btree (create_time);
CREATE INDEX idx_user_workspace_deleted ON public.t_user_workspace USING btree (deleted);
CREATE INDEX idx_user_workspace_user_default ON public.t_user_workspace USING btree (user_id, is_default);
CREATE INDEX idx_user_workspace_user_deleted_default ON public.t_user_workspace USING btree (user_id, deleted, is_default);
CREATE INDEX idx_user_workspace_user_id ON public.t_user_workspace USING btree (user_id);
COMMENT ON TABLE public.t_user_workspace IS '用户工作空间表';

-- Column comments

COMMENT ON COLUMN public.t_user_workspace.id IS '主键ID';
COMMENT ON COLUMN public.t_user_workspace.user_id IS '用户ID（逻辑外键->t_user.id）';
COMMENT ON COLUMN public.t_user_workspace."name" IS '工作空间名称';
COMMENT ON COLUMN public.t_user_workspace.description IS '工作空间描述';
COMMENT ON COLUMN public.t_user_workspace.is_default IS '是否默认工作空间：1是 0否';
COMMENT ON COLUMN public.t_user_workspace.status IS '状态: 1正常 2禁用';
COMMENT ON COLUMN public.t_user_workspace.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_user_workspace.create_time IS '创建时间';
COMMENT ON COLUMN public.t_user_workspace.update_time IS '更新时间';
COMMENT ON COLUMN public.t_user_workspace.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_user_workspace.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_user_workspace_updated_at before
update
    on
    public.t_user_workspace for each row execute function update_updated_at_column();


-- public.t_workspace_chat_task definition

-- Drop table

-- DROP TABLE t_workspace_chat_task;

CREATE TABLE t_workspace_chat_task (
	id bigserial NOT NULL, -- 主键ID
	workspace_id int8 NOT NULL, -- 工作空间ID（逻辑外键->t_user_workspace.id）
	context_id varchar(100) NOT NULL, -- 会话上下文ID，用于与下游平台通信
	title varchar(200) NOT NULL, -- 对话任务标题
	"content" text NULL, -- 对话任务内容
	is_favorite int2 DEFAULT 0 NOT NULL, -- 是否收藏：1是 0否
	status int2 DEFAULT 1 NOT NULL, -- 状态: 1进行中 2已完成 3已归档
	deleted int2 DEFAULT 0 NOT NULL, -- 删除标记：0-未删除，1-已删除
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	create_by int8 NULL, -- 创建人ID
	update_by int8 NULL, -- 更新人ID
	CONSTRAINT t_workspace_chat_task_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_workspace_chat_task_context_id ON public.t_workspace_chat_task USING btree (context_id);
CREATE INDEX idx_workspace_chat_task_create_time ON public.t_workspace_chat_task USING btree (create_time);
CREATE INDEX idx_workspace_chat_task_deleted ON public.t_workspace_chat_task USING btree (deleted);
CREATE INDEX idx_workspace_chat_task_status ON public.t_workspace_chat_task USING btree (status);
CREATE INDEX idx_workspace_chat_task_workspace_deleted_status ON public.t_workspace_chat_task USING btree (workspace_id, deleted, status);
CREATE INDEX idx_workspace_chat_task_workspace_id ON public.t_workspace_chat_task USING btree (workspace_id);
COMMENT ON TABLE public.t_workspace_chat_task IS '工作空间对话任务表';

-- Column comments

COMMENT ON COLUMN public.t_workspace_chat_task.id IS '主键ID';
COMMENT ON COLUMN public.t_workspace_chat_task.workspace_id IS '工作空间ID（逻辑外键->t_user_workspace.id）';
COMMENT ON COLUMN public.t_workspace_chat_task.context_id IS '会话上下文ID，用于与下游平台通信';
COMMENT ON COLUMN public.t_workspace_chat_task.title IS '对话任务标题';
COMMENT ON COLUMN public.t_workspace_chat_task."content" IS '对话任务内容';
COMMENT ON COLUMN public.t_workspace_chat_task.is_favorite IS '是否收藏：1是 0否';
COMMENT ON COLUMN public.t_workspace_chat_task.status IS '状态: 1进行中 2已完成 3已归档';
COMMENT ON COLUMN public.t_workspace_chat_task.deleted IS '删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN public.t_workspace_chat_task.create_time IS '创建时间';
COMMENT ON COLUMN public.t_workspace_chat_task.update_time IS '更新时间';
COMMENT ON COLUMN public.t_workspace_chat_task.create_by IS '创建人ID';
COMMENT ON COLUMN public.t_workspace_chat_task.update_by IS '更新人ID';

-- Table Triggers

create trigger update_t_workspace_chat_task_updated_at before
update
    on
    public.t_workspace_chat_task for each row execute function update_updated_at_column();