package com.noah.superagent.service;

/**
 * 积分过期清理服务接口
 * 
 * 处理不同类型积分的过期清理逻辑
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface CreditExpiryService {

    /**
     * 清理过期的当日积分（1天前创建的）
     *
     * @return 处理的用户数量
     */
    int cleanupExpiredDailyCredits();

    /**
     * 清理过期的活动积分（90天前创建的）
     *
     * @return 处理的用户数量
     */
    int cleanupExpiredActivityCredits();

    /**
     * 清理过期的免费积分（90天前创建的）
     *
     * @return 处理的用户数量
     */
    int cleanupExpiredFreeCredits();

    /**
     * 清理指定用户的过期积分
     *
     * @param userId 用户ID
     * @return 是否成功清理
     */
    boolean cleanupExpiredCreditsForUser(Long userId);
}
