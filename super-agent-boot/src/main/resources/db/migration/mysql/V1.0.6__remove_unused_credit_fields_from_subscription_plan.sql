-- 删除套餐表中无用的积分字段
-- 这些字段将通过配置文件控制，而非数据库存储

-- 删除t_subscription_plan表中的积分相关字段
ALTER TABLE `t_subscription_plan` 
DROP COLUMN `credit_amount`,
DROP COLUMN `monthly_credit_amount`, 
DROP COLUMN `yearly_credit_amount`,
DROP COLUMN `daily_refresh_credit`;
