package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.UserDailyLoginEntity;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.noah.superagent.dao.entity.table.UserDailyLoginEntityTableDef.USER_DAILY_LOGIN_ENTITY;


/**
 * 用户每日登录记录Mapper接口
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper
public interface UserDailyLoginMapper extends BaseMapper<UserDailyLoginEntity> {

    /**
     * 根据用户ID和日期查询当日登录记录
     */
    default UserDailyLoginEntity selectByUserIdAndDate(Long userId, LocalDate loginDate) {
        return selectOneByQuery(QueryWrapper.create()
                .where(USER_DAILY_LOGIN_ENTITY.USER_ID.eq(userId))
                .and(USER_DAILY_LOGIN_ENTITY.LOGIN_DATE.eq(loginDate))
                .and(USER_DAILY_LOGIN_ENTITY.DELETED.eq(0)));
    }

    /**
     * 检查用户当日是否已发放积分
     */
    default boolean isDailyCreditsGranted(Long userId, LocalDate loginDate) {
        UserDailyLoginEntity record = selectByUserIdAndDate(userId, loginDate);
        return record != null && Boolean.TRUE.equals(record.getDailyCreditsGranted());
    }
    
    /**
     * 查询在指定日期范围内登录过的所有用户ID
     *
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     * @return 用户ID列表
     */
    default List<Long> selectUserIdsWithRecentLogin(LocalDate startDate, LocalDate endDate) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(USER_DAILY_LOGIN_ENTITY.USER_ID)
                .from(USER_DAILY_LOGIN_ENTITY)
                .where(USER_DAILY_LOGIN_ENTITY.LOGIN_DATE.between(startDate, endDate))
                .groupBy(USER_DAILY_LOGIN_ENTITY.USER_ID);
        
        List<UserDailyLoginEntity> results = selectListByQuery(queryWrapper);
        return results.stream()
                .map(UserDailyLoginEntity::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }
}
