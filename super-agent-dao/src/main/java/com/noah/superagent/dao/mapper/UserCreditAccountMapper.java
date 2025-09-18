package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.UserCreditAccountEntity;
import org.apache.ibatis.annotations.Mapper;


import static com.noah.superagent.dao.entity.table.UserCreditAccountEntityTableDef.USER_CREDIT_ACCOUNT_ENTITY;

/**
 * 用户积分账户Mapper接口
 *
 * @author 任相鹏
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
        );
    }

    /**
     * 根据用户ID查询积分账户及关联的积分余额明细（使用 Relations 注解）
     */
    default UserCreditAccountEntity selectByUserIdWithRelations(Long userId) {
        return selectOneWithRelationsByQuery(QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.USER_ID.eq(userId))
        );
    }

    /**
     * 根据用户ID更新积分余额（乐观锁）
     */
    default int updateBalanceByUserId(Long userId, UserCreditAccountEntity updateEntity) {
        return updateByQuery(updateEntity, QueryWrapper.create()
                .where(USER_CREDIT_ACCOUNT_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_ACCOUNT_ENTITY.VERSION.eq(updateEntity.getVersion()))
        );
    }

    /**
     * 初始化用户积分账户
     */
    default int insertInitAccount(UserCreditAccountEntity account) {
        return insert(account);
    }

}
