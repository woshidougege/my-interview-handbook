-- 用户每日登录记录表（PostgreSQL版本）
CREATE TABLE t_user_daily_login (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_date DATE NOT NULL,
    login_count INTEGER DEFAULT 1,
    first_login_time TIMESTAMP NOT NULL,
    daily_credits_granted BOOLEAN DEFAULT FALSE,
    daily_credits_amount DECIMAL(10,2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted SMALLINT DEFAULT 0
);

-- 表注释
COMMENT ON TABLE t_user_daily_login IS '用户每日登录记录表';
COMMENT ON COLUMN t_user_daily_login.id IS '主键ID';
COMMENT ON COLUMN t_user_daily_login.user_id IS '用户ID';
COMMENT ON COLUMN t_user_daily_login.login_date IS '登录日期';
COMMENT ON COLUMN t_user_daily_login.login_count IS '当日登录次数';
COMMENT ON COLUMN t_user_daily_login.first_login_time IS '当日首次登录时间';
COMMENT ON COLUMN t_user_daily_login.daily_credits_granted IS '当日积分是否已发放';
COMMENT ON COLUMN t_user_daily_login.daily_credits_amount IS '当日发放的积分数量';
COMMENT ON COLUMN t_user_daily_login.create_time IS '创建时间';
COMMENT ON COLUMN t_user_daily_login.update_time IS '更新时间';
COMMENT ON COLUMN t_user_daily_login.create_by IS '创建人ID';
COMMENT ON COLUMN t_user_daily_login.update_by IS '更新人ID';
COMMENT ON COLUMN t_user_daily_login.deleted IS '删除标记：0-未删除，1-已删除';

-- 创建索引
CREATE UNIQUE INDEX uk_user_daily_login_user_date ON t_user_daily_login(user_id, login_date) WHERE deleted = 0;
CREATE INDEX idx_user_daily_login_user_id ON t_user_daily_login(user_id);
CREATE INDEX idx_user_daily_login_date ON t_user_daily_login(login_date);
CREATE INDEX idx_user_daily_login_credits_granted ON t_user_daily_login(daily_credits_granted);
CREATE INDEX idx_user_daily_login_create_time ON t_user_daily_login(create_time);

-- 创建更新时间触发器
CREATE TRIGGER update_t_user_daily_login_updated_at BEFORE UPDATE ON t_user_daily_login
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
