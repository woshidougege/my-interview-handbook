package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.UserDailyLoginEntity;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

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
}
