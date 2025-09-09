package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.CreditExpiryLogEntity;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

import static com.noah.superagent.dao.entity.table.CreditExpiryLogEntityTableDef.CREDIT_EXPIRY_LOG_ENTITY;

/**
 * 积分过期清理日志Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface CreditExpiryLogMapper extends BaseMapper<CreditExpiryLogEntity> {

    /**
     * 根据用户ID查询过期日志
     */
    default List<CreditExpiryLogEntity> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_EXPIRY_LOG_ENTITY.USER_ID.eq(userId))
                .orderBy(CREDIT_EXPIRY_LOG_ENTITY.PROCESSED_TIME.desc())
        );
    }

    /**
     * 根据积分类型查询过期日志
     */
    default List<CreditExpiryLogEntity> selectByCreditType(CreditTypeEnum creditType) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_EXPIRY_LOG_ENTITY.CREDIT_TYPE.eq(creditType))
                .orderBy(CREDIT_EXPIRY_LOG_ENTITY.PROCESSED_TIME.desc())
        );
    }

    /**
     * 根据过期日期查询过期日志
     */
    default List<CreditExpiryLogEntity> selectByExpireDate(LocalDate expireDate) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_EXPIRY_LOG_ENTITY.EXPIRE_DATE.eq(expireDate))
                .orderBy(CREDIT_EXPIRY_LOG_ENTITY.PROCESSED_TIME.desc())
        );
    }

    /**
     * 根据用户ID和积分类型查询过期日志
     */
    default List<CreditExpiryLogEntity> selectByUserIdAndCreditType(Long userId, CreditTypeEnum creditType) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_EXPIRY_LOG_ENTITY.USER_ID.eq(userId))
                .and(CREDIT_EXPIRY_LOG_ENTITY.CREDIT_TYPE.eq(creditType))
                .orderBy(CREDIT_EXPIRY_LOG_ENTITY.PROCESSED_TIME.desc())
        );
    }

    /**
     * 根据日期范围查询过期日志统计
     */
    default List<CreditExpiryLogEntity> selectByDateRange(LocalDate startDate, LocalDate endDate) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_EXPIRY_LOG_ENTITY.EXPIRE_DATE.between(startDate, endDate))
                .orderBy(CREDIT_EXPIRY_LOG_ENTITY.EXPIRE_DATE.desc())
                .orderBy(CREDIT_EXPIRY_LOG_ENTITY.PROCESSED_TIME.desc())
        );
    }

    /**
     * 插入过期日志
     */
    default int insertExpiryLog(CreditExpiryLogEntity log) {
        return insert(log);
    }
}
