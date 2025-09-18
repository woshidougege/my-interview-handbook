-- super_agent.flyway_schema_history definition

CREATE TABLE `flyway_schema_history` (
                                         `installed_rank` int NOT NULL,
                                         `version` varchar(50) DEFAULT NULL,
                                         `description` varchar(200) NOT NULL,
                                         `type` varchar(20) NOT NULL,
                                         `script` varchar(1000) NOT NULL,
                                         `checksum` int DEFAULT NULL,
                                         `installed_by` varchar(100) NOT NULL,
                                         `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `execution_time` int NOT NULL,
                                         `success` tinyint(1) NOT NULL,
                                         PRIMARY KEY (`installed_rank`),
                                         KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;


-- super_agent.t_credit_account definition

CREATE TABLE `t_credit_account` (
                                    `id` bigint NOT NULL COMMENT '主键ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
                                    `total_balance` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总积分余额',
                                    `total_earned` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '累计获得积分',
                                    `total_spent` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费积分',
                                    `version` int NOT NULL DEFAULT '0' COMMENT '版本号（乐观锁）',
                                    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_id` (`user_id`),
                                    KEY `idx_deleted` (`deleted`),
                                    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分账户表';


-- super_agent.t_credit_deduction_task definition

CREATE TABLE `t_credit_deduction_task` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                           `task_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务ID（唯一标识）',
                                           `user_id` bigint NOT NULL COMMENT '用户ID',
                                           `amount` decimal(10,2) NOT NULL COMMENT '扣减金额',
                                           `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扣减描述',
                                           `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
                                           `resource_usage_record_id` bigint DEFAULT NULL COMMENT '关联的资源使用记录ID',
                                           `status` tinyint NOT NULL DEFAULT '0' COMMENT '任务状态：0-待处理，1-处理中，2-成功，3-失败',
                                           `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
                                           `max_retry_count` int NOT NULL DEFAULT '3' COMMENT '最大重试次数',
                                           `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
                                           `last_error_time` datetime DEFAULT NULL COMMENT '最后错误时间',
                                           `scheduled_time` datetime NOT NULL COMMENT '计划执行时间',
                                           `executed_time` datetime DEFAULT NULL COMMENT '实际执行时间',
                                           `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
                                           `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `deleted` tinyint DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                           `version` int DEFAULT '0' COMMENT '版本号（乐观锁）',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `task_id` (`task_id`),
                                           KEY `idx_user_id` (`user_id`),
                                           KEY `idx_status` (`status`),
                                           KEY `idx_scheduled_time` (`scheduled_time`),
                                           KEY `idx_create_time` (`create_time`),
                                           KEY `idx_resource_usage_record_id` (`resource_usage_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分扣减任务表';


-- super_agent.t_credit_expiry_log definition

CREATE TABLE `t_credit_expiry_log` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID',
                                       `credit_type` varchar(32) NOT NULL COMMENT '积分类型代码',
                                       `expired_amount` decimal(15,2) NOT NULL COMMENT '过期积分数量',
                                       `expire_date` date NOT NULL COMMENT '过期日期',
                                       `original_transaction_id` bigint DEFAULT NULL COMMENT '原始积分交易记录ID',
                                       `processed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
                                       `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                       `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_expire_date` (`expire_date`),
                                       KEY `idx_credit_type` (`credit_type`),
                                       KEY `idx_deleted` (`deleted`),
                                       KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分过期清理日志表';


-- super_agent.t_credit_transaction definition

CREATE TABLE `t_credit_transaction` (
                                        `id` bigint NOT NULL COMMENT '主键ID',
                                        `user_id` bigint NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
                                        `transaction_type` tinyint NOT NULL COMMENT '交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零',
                                        `credit_type` varchar(32) DEFAULT NULL COMMENT '积分类型代码',
                                        `amount` decimal(15,2) NOT NULL COMMENT '交易金额（正数表示收入，负数表示支出）',
                                        `balance_before` decimal(15,2) NOT NULL COMMENT '交易前余额',
                                        `balance_after` decimal(15,2) NOT NULL COMMENT '交易后余额',
                                        `description` varchar(255) DEFAULT NULL COMMENT '交易描述',
                                        `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
                                        `related_subscription_id` bigint DEFAULT NULL COMMENT '关联订阅ID（逻辑外键->t_user_subscription.id）',
                                        `expire_time` datetime DEFAULT NULL COMMENT '过期时间（包月积分）',
                                        `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                        `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_user_id` (`user_id`),
                                        KEY `idx_transaction_type` (`transaction_type`),
                                        KEY `idx_credit_type` (`credit_type`),
                                        KEY `idx_deleted` (`deleted`),
                                        KEY `idx_user_deleted_time` (`user_id`,`deleted`,`create_time`),
                                        KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分交易记录表';


-- super_agent.t_credit_type_config definition

CREATE TABLE `t_credit_type_config` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `type_code` varchar(32) NOT NULL COMMENT '积分类型代码',
                                        `type_name` varchar(64) NOT NULL COMMENT '积分类型名称',
                                        `validity_days` int NOT NULL DEFAULT '0' COMMENT '有效期天数，0表示永久',
                                        `consume_priority` int NOT NULL COMMENT '消费优先级，数字越小优先级越高',
                                        `description` varchar(200) DEFAULT NULL COMMENT '描述',
                                        `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
                                        `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                        `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_type_code` (`type_code`),
                                        KEY `idx_consume_priority` (`consume_priority`),
                                        KEY `idx_enabled` (`enabled`),
                                        KEY `idx_deleted` (`deleted`),
                                        KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分类型配置表';


-- super_agent.t_payment_record definition

CREATE TABLE `t_payment_record` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `order_id` bigint NOT NULL COMMENT '订单ID',
                                    `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
                                    `payment_method` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付方式',
                                    `third_party_order_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方支付订单号',
                                    `third_party_transaction_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方交易流水号',
                                    `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '支付状态',
                                    `qr_code` text COLLATE utf8mb4_unicode_ci COMMENT '支付二维码',
                                    `payment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付链接',
                                    `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
                                    `callback_data` text COLLATE utf8mb4_unicode_ci COMMENT '第三方回调数据',
                                    `failure_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
                                    `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
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


-- super_agent.t_resource_usage_record definition

CREATE TABLE `t_resource_usage_record` (
                                           `id` bigint NOT NULL COMMENT '主键ID',
                                           `request_id` varchar(100) NOT NULL COMMENT '请求ID（幂等键）',
                                           `report_id` varchar(50) NOT NULL COMMENT '报告ID',
                                           `user_id` bigint NOT NULL COMMENT '用户ID（雪花算法生成）',
                                           `agent_id` varchar(100) NOT NULL COMMENT '智能体ID',
                                           `context_id` varchar(100) DEFAULT NULL COMMENT '会话ID',
                                           `task_type` varchar(50) NOT NULL COMMENT '任务类型：INDUSTRY_RESEARCH_REPORT, PPT_GENERATION等',
                                           `task_description` varchar(500) DEFAULT NULL COMMENT '任务描述',
                                           `resource_type` varchar(20) NOT NULL COMMENT '资源类型：MODEL, FUNCTION, MEDIA',
                                           `resource_name` varchar(100) NOT NULL COMMENT '资源名称：模型名称、功能类型、媒体类型等',
                                           `resource_subtype` varchar(50) DEFAULT NULL COMMENT '资源子类型：TEXT_GENERATION, IMAGE_GENERATION等',
                                           `usage_data` text COMMENT '使用量JSON数据（Token数、次数、秒数等）',
                                           `billing_unit` varchar(20) DEFAULT NULL COMMENT '计费单位：TOKEN, TIMES, PAGES, COUNT, SECONDS',
                                           `usage_amount` decimal(15,6) DEFAULT '0.000000' COMMENT '使用量',
                                           `unit_price` decimal(10,6) DEFAULT '0.000000' COMMENT '单价（元）',
                                           `billing_amount` decimal(15,6) DEFAULT '0.000000' COMMENT '计费金额（元）',
                                           `description` varchar(500) DEFAULT NULL COMMENT '描述',
                                           `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                           `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                           `deleted` tinyint DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_resource_usage_request_id` (`request_id`),
                                           KEY `idx_resource_usage_user_id` (`user_id`),
                                           KEY `idx_resource_usage_agent_id` (`agent_id`),
                                           KEY `idx_resource_usage_context_id` (`context_id`),
                                           KEY `idx_resource_usage_report_id` (`report_id`),
                                           KEY `idx_resource_usage_task_type` (`task_type`),
                                           KEY `idx_resource_usage_resource_type` (`resource_type`),
                                           KEY `idx_resource_usage_create_time` (`create_time`),
                                           KEY `idx_resource_usage_user_time` (`user_id`,`create_time`),
                                           KEY `idx_resource_usage_task_resource` (`task_type`,`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='资源使用记录表';


-- super_agent.t_schedule_execution_log definition

CREATE TABLE `t_schedule_execution_log` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                            `task_name` varchar(200) NOT NULL COMMENT '任务名称',
                                            `task_instance` varchar(200) NOT NULL COMMENT '任务实例ID',
                                            `execution_time` timestamp(6) NOT NULL COMMENT '计划执行时间',
                                            `actual_execution_time` timestamp(6) NULL DEFAULT NULL COMMENT '实际执行时间',
                                            `completion_time` timestamp(6) NULL DEFAULT NULL COMMENT '完成时间',
                                            `success` tinyint(1) DEFAULT NULL COMMENT '是否执行成功',
                                            `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时（毫秒）',
                                            `result_message` text COMMENT '执行结果消息',
                                            `error_message` text COMMENT '错误信息',
                                            `executor_id` varchar(50) DEFAULT NULL COMMENT '执行者ID',
                                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_task_name` (`task_name`),
                                            KEY `idx_execution_time` (`execution_time`),
                                            KEY `idx_create_time` (`create_time`),
                                            KEY `idx_success` (`success`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务执行日志表';


-- super_agent.t_scheduled_chat_task definition

CREATE TABLE `t_scheduled_chat_task` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `workspace_id` bigint NOT NULL COMMENT '工作空间ID',
                                         `chat_task_id` bigint DEFAULT NULL COMMENT '对话任务ID',
                                         `task_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
                                         `cron_expression` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Cron表达式',
                                         `prompt` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对话提示词',
                                         `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1启用 0禁用',
                                         `last_execution_time` datetime DEFAULT NULL COMMENT '上次执行时间',
                                         `next_execution_time` datetime DEFAULT NULL COMMENT '下次执行时间',
                                         `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型: 0-一次性任务 1-可重复任务',
                                         `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                         `schedule_config` text COLLATE utf8mb4_unicode_ci COMMENT '任务调度配置（JSON格式存储）',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                         `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_scheduled_chat_task_user_id` (`user_id`),
                                         KEY `idx_scheduled_chat_task_workspace_id` (`workspace_id`),
                                         KEY `idx_scheduled_chat_task_chat_task_id` (`chat_task_id`),
                                         KEY `idx_scheduled_chat_task_status` (`status`),
                                         KEY `idx_scheduled_chat_task_next_execution_time` (`next_execution_time`),
                                         KEY `idx_scheduled_chat_task_deleted` (`deleted`),
                                         KEY `idx_scheduled_chat_task_user_deleted_status` (`user_id`,`deleted`,`status`),
                                         KEY `idx_scheduled_chat_task_schedule_config` (`schedule_config`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时对话任务表';


-- super_agent.t_scheduled_chat_task_execution_log definition

CREATE TABLE `t_scheduled_chat_task_execution_log` (
                                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                       `task_id` bigint NOT NULL COMMENT '定时任务ID',
                                                       `chat_task_id` bigint DEFAULT NULL COMMENT '对话任务ID',
                                                       `task_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
                                                       `start_time` datetime NOT NULL COMMENT '执行开始时间',
                                                       `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
                                                       `execution_status` tinyint NOT NULL COMMENT '执行状态: 1成功 0失败',
                                                       `execution_result` text COLLATE utf8mb4_unicode_ci COMMENT '执行结果',
                                                       `duration` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
                                                       `error_message` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
                                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                       `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                                       `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                                       `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                                       PRIMARY KEY (`id`),
                                                       KEY `idx_scheduled_chat_task_execution_log_task_id` (`task_id`),
                                                       KEY `idx_scheduled_chat_task_execution_log_chat_task_id` (`chat_task_id`),
                                                       KEY `idx_scheduled_chat_task_execution_log_task_name` (`task_name`),
                                                       KEY `idx_scheduled_chat_task_execution_log_start_time` (`start_time`),
                                                       KEY `idx_scheduled_chat_task_execution_log_execution_status` (`execution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时对话任务执行日志表';


-- super_agent.t_scheduled_execution_logs definition

CREATE TABLE `t_scheduled_execution_logs` (
                                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                              `task_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称（扩展到128字符）',
                                              `task_instance` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务实例ID（扩展到256字符）',
                                              `task_data` blob COMMENT '任务数据',
                                              `picked_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行者标识',
                                              `time_started` timestamp(6) NOT NULL COMMENT '任务开始执行时间',
                                              `time_finished` timestamp(6) NOT NULL COMMENT '任务完成时间',
                                              `succeeded` tinyint(1) NOT NULL COMMENT '任务是否执行成功',
                                              `duration_ms` bigint NOT NULL COMMENT '任务执行耗时（毫秒）',
                                              `exception_class` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常类名（缩短到500字符以支持索引）',
                                              `exception_message` blob COMMENT '异常消息',
                                              `exception_stacktrace` blob COMMENT '异常堆栈信息',
                                              PRIMARY KEY (`id`),
                                              KEY `idx_t_scheduled_execution_logs_started` (`time_started`),
                                              KEY `idx_t_scheduled_execution_logs_task_name` (`task_name`),
                                              KEY `idx_t_scheduled_execution_logs_exception_class` (`exception_class`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='db-scheduler 任务执行历史记录表（标准格式）';


-- super_agent.t_scheduled_task definition

CREATE TABLE `t_scheduled_task` (
                                    `task_name` varchar(128) NOT NULL COMMENT '任务名称（扩展到128字符）',
                                    `task_instance` varchar(256) NOT NULL COMMENT '任务实例ID（256字符支持长订单号）',
                                    `task_data` blob COMMENT '任务数据',
                                    `execution_time` timestamp(6) NOT NULL COMMENT '执行时间',
                                    `picked` tinyint(1) NOT NULL COMMENT '是否被选中执行',
                                    `picked_by` varchar(50) DEFAULT NULL COMMENT '执行者标识',
                                    `last_success` timestamp(6) NULL DEFAULT NULL COMMENT '最后成功时间',
                                    `last_failure` timestamp(6) NULL DEFAULT NULL COMMENT '最后失败时间',
                                    `consecutive_failures` int DEFAULT NULL COMMENT '连续失败次数',
                                    `last_heartbeat` timestamp(6) NULL DEFAULT NULL COMMENT '最后心跳时间',
                                    `version` bigint NOT NULL COMMENT '版本号（乐观锁）',
                                    PRIMARY KEY (`task_name`,`task_instance`),
                                    KEY `execution_time_idx` (`execution_time`),
                                    KEY `last_heartbeat_idx` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';


-- super_agent.t_subscription_order definition

CREATE TABLE `t_subscription_order` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `plan_id` bigint NOT NULL COMMENT '套餐ID',
                                        `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐名称',
                                        `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
                                        `billing_cycle` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'monthly' COMMENT '计费周期',
                                        `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '订单状态',
                                        `payment_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付方式',
                                        `third_party_order_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方支付订单号',
                                        `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
                                        `expired_at` datetime NOT NULL COMMENT '过期时间',
                                        `effective_start_time` datetime DEFAULT NULL COMMENT '套餐生效开始时间',
                                        `effective_end_time` datetime DEFAULT NULL COMMENT '套餐生效结束时间',
                                        `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                        `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                        `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_order_no` (`order_no`),
                                        KEY `idx_user_id` (`user_id`),
                                        KEY `idx_plan_id` (`plan_id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_deleted` (`deleted`),
                                        KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅订单表';


-- super_agent.t_user_credit_balance definition

CREATE TABLE `t_user_credit_balance` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `credit_type` varchar(32) NOT NULL COMMENT '积分类型代码',
                                         `balance` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '余额',
                                         `total_earned` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '累计获得',
                                         `total_spent` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费',
                                         `last_earn_time` datetime DEFAULT NULL COMMENT '最后获得时间',
                                         `last_spend_time` datetime DEFAULT NULL COMMENT '最后消费时间',
                                         `version` int NOT NULL DEFAULT '0' COMMENT '版本号（乐观锁）',
                                         `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                         `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_user_credit_type` (`user_id`,`credit_type`,`deleted`),
                                         KEY `idx_user_id` (`user_id`),
                                         KEY `idx_credit_type` (`credit_type`),
                                         KEY `idx_balance` (`balance`),
                                         KEY `idx_deleted` (`deleted`),
                                         KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分余额明细表';


-- super_agent.t_user_daily_login definition

CREATE TABLE `t_user_daily_login` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `login_date` date NOT NULL COMMENT '登录日期',
                                      `login_count` int DEFAULT '1' COMMENT '当日登录次数',
                                      `first_login_time` datetime NOT NULL COMMENT '当日首次登录时间',
                                      `daily_credits_granted` tinyint(1) DEFAULT '0' COMMENT '当日积分是否已发放',
                                      `daily_credits_amount` decimal(10,2) DEFAULT '0.00' COMMENT '当日发放的积分数量',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                      `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                      `deleted` tinyint DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_user_daily_login_user_date` (`user_id`,`login_date`,`deleted`),
                                      KEY `idx_user_daily_login_user_id` (`user_id`),
                                      KEY `idx_user_daily_login_date` (`login_date`),
                                      KEY `idx_user_daily_login_credits_granted` (`daily_credits_granted`),
                                      KEY `idx_user_daily_login_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户每日登录记录表';


-- super_agent.t_user_subscription definition

CREATE TABLE `t_user_subscription` (
                                       `id` bigint NOT NULL COMMENT '主键ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
                                       `plan_id` bigint NOT NULL COMMENT '套餐ID（逻辑外键->t_subscription_plan.id）',
                                       `start_time` datetime NOT NULL COMMENT '订阅开始时间',
                                       `end_time` datetime NOT NULL COMMENT '订阅结束时间',
                                       `paid_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
                                       `credit_amount` decimal(15,2) NOT NULL COMMENT '获得积分数量',
                                       `status` tinyint NOT NULL DEFAULT '1' COMMENT '订阅状态 1-生效中 2-已过期 3-已取消',
                                       `pay_order_no` varchar(64) DEFAULT NULL COMMENT '支付订单号',
                                       `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                                       `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                       `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_plan_id` (`plan_id`),
                                       KEY `idx_status_end_time` (`status`,`end_time`),
                                       KEY `idx_deleted` (`deleted`),
                                       KEY `idx_user_deleted_status` (`user_id`,`deleted`,`status`),
                                       KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户订阅记录表';


-- super_agent.t_user_workspace definition

CREATE TABLE `t_user_workspace` (
                                    `id` bigint NOT NULL COMMENT '主键ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID（逻辑外键->t_user.id）',
                                    `name` varchar(100) NOT NULL COMMENT '工作空间名称',
                                    `description` text COMMENT '工作空间描述',
                                    `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认工作空间：1是 0否',
                                    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1正常 2禁用',
                                    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_user_default` (`user_id`,`is_default`),
                                    KEY `idx_deleted` (`deleted`),
                                    KEY `idx_user_deleted_default` (`user_id`,`deleted`,`is_default`),
                                    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户工作空间表';


-- super_agent.t_workspace_chat_task definition

CREATE TABLE `t_workspace_chat_task` (
                                         `id` bigint NOT NULL COMMENT '主键ID',
                                         `workspace_id` bigint NOT NULL COMMENT '工作空间ID（逻辑外键->t_user_workspace.id）',
                                         `context_id` varchar(100) NOT NULL COMMENT '会话上下文ID，用于与下游平台通信',
                                         `title` varchar(200) NOT NULL COMMENT '对话任务标题',
                                         `content` text COMMENT '对话任务内容',
                                         `is_favorite` tinyint NOT NULL DEFAULT '0' COMMENT '是否收藏：1是 0否',
                                         `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1进行中 2已完成 3已归档',
                                         `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
                                         `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_workspace_id` (`workspace_id`),
                                         KEY `idx_context_id` (`context_id`),
                                         KEY `idx_status` (`status`),
                                         KEY `idx_deleted` (`deleted`),
                                         KEY `idx_workspace_deleted_status` (`workspace_id`,`deleted`,`status`),
                                         KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间对话任务表';