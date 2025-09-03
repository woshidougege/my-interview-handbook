package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.CreditTransaction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 积分交易记录Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface CreditTransactionMapper extends BaseMapper<CreditTransaction> {

    /**
     * 根据用户ID查询积分交易记录列表
     */
    default List<CreditTransaction> selectByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CreditTransaction::getUserId).eq(userId)
                .orderBy(CreditTransaction::getCreateTime).desc());
    }

    /**
     * 分页查询积分交易记录
     */
    default Page<CreditTransaction> selectTransactionPage(Page<CreditTransaction> page, Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .where(CreditTransaction::getUserId).eq(userId)
                .orderBy(CreditTransaction::getCreateTime).desc();

        return paginate(page, query);
    }

    /**
     * 根据交易类型查询积分交易记录列表
     */
    default List<CreditTransaction> selectByType(Long userId, Integer type) {
        return selectListByQuery(QueryWrapper.create()
                .where(CreditTransaction::getUserId).eq(userId)
                .and(CreditTransaction::getTransactionType).eq(type)
                .orderBy(CreditTransaction::getCreateTime).desc());
    }

    /**
     * 根据时间范围查询积分交易记录列表
     */
    default List<CreditTransaction> selectByTimeRange(Long userId, java.util.Date startTime, java.util.Date endTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(CreditTransaction::getUserId).eq(userId)
                .and(CreditTransaction::getCreateTime).ge(startTime)
                .and(CreditTransaction::getCreateTime).le(endTime)
                .orderBy(CreditTransaction::getCreateTime).desc());
    }

    /**
     * 统计用户总支出积分
     */
    default java.math.BigDecimal sumExpenseByUserId(Long userId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(CreditTransaction::getAmount)
                .where(CreditTransaction::getUserId).eq(userId)
                .and(CreditTransaction::getAmount).lt(0), 
                java.math.BigDecimal.class);
    }

    /**
     * 统计用户总收入积分
     */
    default java.math.BigDecimal sumIncomeByUserId(Long userId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(CreditTransaction::getAmount)
                .where(CreditTransaction::getUserId).eq(userId)
                .and(CreditTransaction::getAmount).gt(0), 
                java.math.BigDecimal.class);
    }

    /**
     * 根据交易类型和时间范围查询积分交易记录列表
     */
    default List<CreditTransaction> selectByTypeAndTimeRange(Long userId, Integer type, java.util.Date startTime, java.util.Date endTime) {
        return selectListByQuery(QueryWrapper.create()
                .where(CreditTransaction::getUserId).eq(userId)
                .and(CreditTransaction::getTransactionType).eq(type)
                .and(CreditTransaction::getCreateTime).ge(startTime)
                .and(CreditTransaction::getCreateTime).le(endTime)
                .orderBy(CreditTransaction::getCreateTime).desc());
    }

    /**
     * 查询最近的积分交易记录
     */
    default List<CreditTransaction> selectRecentTransactions(Long userId, int limit) {
        return selectListByQuery(QueryWrapper.create()
                .where(CreditTransaction::getUserId).eq(userId)
                .orderBy(CreditTransaction::getCreateTime).desc()
                .limit(limit));
    }

    /**
     * 统计指定时间段内的交易总额
     */
    default java.math.BigDecimal sumAmountByTimeRange(Long userId, java.util.Date startTime, java.util.Date endTime) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(CreditTransaction::getAmount)
                .where(CreditTransaction::getUserId).eq(userId)
                .and(CreditTransaction::getCreateTime).ge(startTime)
                .and(CreditTransaction::getCreateTime).le(endTime),
                java.math.BigDecimal.class);
    }
}