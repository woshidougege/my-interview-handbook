package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.UserCreditBalanceEntity;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

import static com.noah.superagent.dao.entity.table.UserCreditBalanceEntityTableDef.USER_CREDIT_BALANCE_ENTITY;

/**
 * 用户积分余额明细Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface UserCreditBalanceMapper extends BaseMapper<UserCreditBalanceEntity> {

    /**
     * 根据用户ID和积分类型查询积分余额
     */
    default UserCreditBalanceEntity selectByUserIdAndCreditType(Long userId, CreditTypeEnum creditType) {
        return selectOneByQuery(QueryWrapper.create()
                .where(USER_CREDIT_BALANCE_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_BALANCE_ENTITY.CREDIT_TYPE.eq(creditType))
                .and(USER_CREDIT_BALANCE_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 根据用户ID查询所有积分余额，按消费优先级排序
     */
    default List<UserCreditBalanceEntity> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .select(USER_CREDIT_BALANCE_ENTITY.ALL_COLUMNS)
                .from(USER_CREDIT_BALANCE_ENTITY)
                .leftJoin("t_credit_type_config").as("config")
                .on(USER_CREDIT_BALANCE_ENTITY.CREDIT_TYPE.eq("config.type_code"))
                .where(USER_CREDIT_BALANCE_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_BALANCE_ENTITY.DELETED.eq(0))
                .and("config.enabled = 1")
                .orderBy("config.consume_priority", true)
        );
    }

    /**
     * 根据用户ID查询有余额的积分类型
     */
    default List<UserCreditBalanceEntity> selectBalancesWithAmount(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(USER_CREDIT_BALANCE_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_BALANCE_ENTITY.BALANCE.gt(BigDecimal.ZERO))
                .and(USER_CREDIT_BALANCE_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 根据积分类型查询所有有余额的用户
     */
    default List<UserCreditBalanceEntity> selectUsersByCreditType(CreditTypeEnum creditType) {
        return selectListByQuery(QueryWrapper.create()
                .where(USER_CREDIT_BALANCE_ENTITY.CREDIT_TYPE.eq(creditType))
                .and(USER_CREDIT_BALANCE_ENTITY.BALANCE.gt(BigDecimal.ZERO))
                .and(USER_CREDIT_BALANCE_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 查询所有有余额的用户积分记录（用于过期清理）
     */
    default List<UserCreditBalanceEntity> selectAllWithBalance() {
        return selectListByQuery(QueryWrapper.create()
                .where(USER_CREDIT_BALANCE_ENTITY.BALANCE.gt(BigDecimal.ZERO))
                .and(USER_CREDIT_BALANCE_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 根据用户ID和积分类型更新积分余额（乐观锁）
     */
    default int updateBalanceByUserIdAndCreditType(Long userId, CreditTypeEnum creditType, 
                                                  UserCreditBalanceEntity updateEntity) {
        return updateByQuery(updateEntity, QueryWrapper.create()
                .where(USER_CREDIT_BALANCE_ENTITY.USER_ID.eq(userId))
                .and(USER_CREDIT_BALANCE_ENTITY.CREDIT_TYPE.eq(creditType))
                .and(USER_CREDIT_BALANCE_ENTITY.VERSION.eq(updateEntity.getVersion()))
                .and(USER_CREDIT_BALANCE_ENTITY.DELETED.eq(0))
        );
    }

    /**
     * 初始化用户积分余额记录
     */
    default int insertOrUpdate(UserCreditBalanceEntity entity) {
        UserCreditBalanceEntity existing = selectByUserIdAndCreditType(
                entity.getUserId(), entity.getCreditType());
        
        if (existing == null) {
            return insert(entity);
        } else {
            // 更新现有记录
            entity.setId(existing.getId());
            entity.setVersion(existing.getVersion());
            return updateByQuery(entity, QueryWrapper.create()
                    .where(USER_CREDIT_BALANCE_ENTITY.ID.eq(existing.getId()))
                    .and(USER_CREDIT_BALANCE_ENTITY.VERSION.eq(existing.getVersion())));
        }
    }
}
