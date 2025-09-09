package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.noah.superagent.dao.entity.table.UserCreditAccountEntityTableDef.USER_CREDIT_ACCOUNT_ENTITY;

/**
 * 用户积分账户Mapper接口
 *
 * @author Noah
 * @since 1.0.0
 */
@Mapper
public interface UserCreditAccountMapper extends BaseMapper<UserCreditAccountEntity> {

    /**
     * 根据用户ID查询积分账户
     */
    default UserCreditAccountEntity selectByUserId(Long userId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_ACCOUNT_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 根据用户ID更新积分余额（乐观锁）
     */
    default int updateBalanceByUserId(Long userId, UserCreditAccountEntity updateEntity) {
        return updateByQuery(updateEntity, QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_ACCOUNT_ENTITY.VERSION.eq(updateEntity.getVersion()))
                .and(USER_CREDIT_ACCOUNT_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 初始化用户积分账户
     */
    default int insertInitAccount(UserCreditAccountEntity account) {
        return insert(account);
    }

    /**
     * 查询有当日积分余额的用户
     */
    default List<UserCreditAccountEntity> selectUsersWithDailyBalance() {
        return selectListByQuery(QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.DAILY_BALANCE.gt(0))
                .and(USER_CREDIT_ACCOUNT_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 查询有活动积分余额的用户
     */
    default List<UserCreditAccountEntity> selectUsersWithActivityBalance() {
        return selectListByQuery(QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.ACTIVITY_BALANCE.gt(0))
                .and(USER_CREDIT_ACCOUNT_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 查询有免费积分余额的用户
     */
    default List<UserCreditAccountEntity> selectUsersWithFreeBalance() {
        return selectListByQuery(QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.FREE_BALANCE.gt(0))
                .and(USER_CREDIT_ACCOUNT_ENTITY.DELETED.eq(0))
        );
    }
}
