package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.UserSubscription;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户订阅记录Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface UserSubscriptionMapper extends BaseMapper<UserSubscription> {

    /**
     * 根据用户ID查询订阅记录列表
     */
    default List<UserSubscription> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserSubscription::getUserId).eq(userId)
                .orderBy(UserSubscription::getStartTime).desc());
    }

    /**
     * 分页查询用户订阅记录
     */
    default Page<UserSubscription> selectSubscriptionPage(Page<UserSubscription> page, Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .where(UserSubscription::getUserId).eq(userId)
                .orderBy(UserSubscription::getStartTime).desc();

        return paginate(page, query);
    }

    /**
     * 根据套餐ID查询订阅记录列表
     */
    default List<UserSubscription> selectByPlanId(Long planId) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserSubscription::getPlanId).eq(planId));
    }

    /**
     * 根据订阅状态查询用户订阅记录列表
     */
    default List<UserSubscription> selectByStatus(Long userId, Integer status) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserSubscription::getUserId).eq(userId)
                .and(UserSubscription::getStatus).eq(status)
                .orderBy(UserSubscription::getStartTime).desc());
    }

    /**
     * 根据时间范围查询用户订阅记录列表
     */
    default List<UserSubscription> selectByTimeRange(Long userId, java.util.Date startTime, java.util.Date endTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserSubscription::getUserId).eq(userId)
                .and(UserSubscription::getStartTime).ge(startTime)
                .and(UserSubscription::getEndTime).le(endTime)
                .orderBy(UserSubscription::getStartTime).desc());
    }

    /**
     * 查询即将过期的订阅记录
     */
    default List<UserSubscription> selectExpiringSubscriptions(java.util.Date beforeDate) {
        return selectListByQuery(QueryWrapper.create()
                .where(UserSubscription::getEndTime).le(beforeDate)
                .and(UserSubscription::getStatus).eq(1)
                .orderBy(UserSubscription::getEndTime).asc());
    }

    /**
     * 更新订阅状态
     */
    default int updateStatusById(Long id, Integer status) {
        // 创建仅包含状态字段的更新对象
        UserSubscription updateSubscription = new UserSubscription();
        updateSubscription.setStatus(status);
        return updateByQuery(updateSubscription, 
                QueryWrapper.create().where(UserSubscription::getId).eq(id));
    }
}
