-- 为订阅套餐表添加套餐代码字段
ALTER TABLE t_subscription_plan 
ADD COLUMN plan_code VARCHAR(32);

COMMENT ON COLUMN t_subscription_plan.plan_code IS '套餐代码（英文标识）';

-- 更新现有数据的套餐代码
UPDATE t_subscription_plan SET plan_code = 'free' WHERE id = 1;
UPDATE t_subscription_plan SET plan_code = 'basic' WHERE id = 2;
UPDATE t_subscription_plan SET plan_code = 'premium' WHERE id = 3;
UPDATE t_subscription_plan SET plan_code = 'credit_pack' WHERE id = 4;

-- 现在设置为NOT NULL（所有记录都已经有值了）
ALTER TABLE t_subscription_plan 
ALTER COLUMN plan_code SET NOT NULL;

-- 为套餐代码字段添加唯一索引
CREATE UNIQUE INDEX uk_plan_code ON t_subscription_plan(plan_code, deleted);
