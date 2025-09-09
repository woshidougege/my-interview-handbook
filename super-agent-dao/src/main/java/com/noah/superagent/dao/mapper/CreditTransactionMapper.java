package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.dao.entity.CreditTransactionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

import static com.noah.superagent.dao.entity.table.CreditTransactionEntityTableDef.CREDIT_TRANSACTION_ENTITY;

/**
 * 积分交易记录Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface CreditTransactionMapper extends BaseMapper<CreditTransactionEntity> {

    /**
     * 根据用户ID分页查询交易记录
     */
    default Page<CreditTransactionEntity> selectPageByUserId(Page<CreditTransactionEntity> page, Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .where(CREDIT_TRANSACTION_ENTITY.USER_ID.eq(userId))
                .and(CREDIT_TRANSACTION_ENTITY.DELETED.eq(0))
                .orderBy(CREDIT_TRANSACTION_ENTITY.CREATE_TIME.desc());
        
        return paginate(page, query);
    }

    /**
     * 根据用户ID和交易类型查询记录
     */
    default List<CreditTransactionEntity> selectByUserIdAndType(Long userId, CreditTransactionTypeEnum transactionType) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TRANSACTION_ENTITY.USER_ID.eq(userId))
                .and(CREDIT_TRANSACTION_ENTITY.TRANSACTION_TYPE.eq(transactionType))
                .and(CREDIT_TRANSACTION_ENTITY.DELETED.eq(0))
                .orderBy(CREDIT_TRANSACTION_ENTITY.CREATE_TIME.desc())
        );
    }

    /**
     * 根据用户ID和时间范围查询交易记录
     */
    default List<CreditTransactionEntity> selectByUserIdAndTimeRange(Long userId, 
            LocalDateTime startTime, LocalDateTime endTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TRANSACTION_ENTITY.USER_ID.eq(userId))
                .and(CREDIT_TRANSACTION_ENTITY.CREATE_TIME.between(startTime, endTime))
                .and(CREDIT_TRANSACTION_ENTITY.DELETED.eq(0))
                .orderBy(CREDIT_TRANSACTION_ENTITY.CREATE_TIME.desc())
        );
    }

    /**
     * 插入交易记录
     */
    default int insertTransaction(CreditTransactionEntity transaction) {
        return insert(transaction);
    }

    /**
     * 查询过期的交易记录
     */
    default List<CreditTransactionEntity> selectExpiredTransactions(Long userId, 
            CreditTransactionTypeEnum transactionType, LocalDateTime expireThreshold) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TRANSACTION_ENTITY.USER_ID.eq(userId))
                .and(CREDIT_TRANSACTION_ENTITY.TRANSACTION_TYPE.eq(transactionType))
                .and(CREDIT_TRANSACTION_ENTITY.CREATE_TIME.lt(expireThreshold))
                .and(CREDIT_TRANSACTION_ENTITY.AMOUNT.gt(0)) // 只查询收入记录
                .and(CREDIT_TRANSACTION_ENTITY.DELETED.eq(0))
                .orderBy(CREDIT_TRANSACTION_ENTITY.CREATE_TIME.asc())
        );
    }
}
