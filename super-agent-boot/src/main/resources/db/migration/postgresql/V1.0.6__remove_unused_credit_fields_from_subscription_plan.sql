-- 删除套餐表中无用的积分字段
-- 这些字段将通过配置文件控制，而非数据库存储

-- 删除t_subscription_plan表中的积分相关字段
ALTER TABLE t_subscription_plan 
DROP COLUMN IF EXISTS credit_amount,
DROP COLUMN IF EXISTS monthly_credit_amount, 
DROP COLUMN IF EXISTS yearly_credit_amount,
DROP COLUMN IF EXISTS daily_refresh_credit;
