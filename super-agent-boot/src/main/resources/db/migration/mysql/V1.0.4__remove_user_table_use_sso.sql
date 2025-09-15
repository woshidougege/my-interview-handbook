-- 删除用户表，直接使用SSO获取用户信息
-- 创建时间：2025-09-15
-- 说明：移除本地用户表，用户信息完全通过SSO获取
-- 业务表的user_id字段保持BIGINT类型，SSO的int会自动转换为long

-- 删除用户表（不再需要本地存储用户信息）
DROP TABLE IF EXISTS `t_user`;
