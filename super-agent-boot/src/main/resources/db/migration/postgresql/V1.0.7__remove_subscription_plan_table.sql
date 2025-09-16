-- 删除套餐表，改为配置文件管理
-- 注意：此操作需要在确保相关业务逻辑已经迁移到配置文件后执行

-- 删除订阅套餐表（改为配置文件管理）
DROP TABLE IF EXISTS t_subscription_plan;
