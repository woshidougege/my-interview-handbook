package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.SubscriptionPlan;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 订阅套餐Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface SubscriptionPlanMapper extends BaseMapper<SubscriptionPlan> {

    /**
     * 查询启用的订阅套餐列表
     */
    default List<SubscriptionPlan> selectEnabledPlans() {
        return selectListByQuery(QueryWrapper.create()
                .where(SubscriptionPlan::getEnabled).eq(1)
                .orderBy(SubscriptionPlan::getSortOrder).asc());
    }


    /**
     * 根据价格范围查询订阅套餐列表
     */
    default List<SubscriptionPlan> selectByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        return selectListByQuery(QueryWrapper.create()
                .where(SubscriptionPlan::getPrice).ge(minPrice)
                .and(SubscriptionPlan::getPrice).le(maxPrice)
                .orderBy(SubscriptionPlan::getPrice).asc());
    }

    /**
     * 根据积分数量查询订阅套餐列表
     */
    default List<SubscriptionPlan> selectByCreditAmount(java.math.BigDecimal creditAmount) {
        return selectListByQuery(QueryWrapper.create()
                .where(SubscriptionPlan::getCreditAmount).eq(creditAmount)
                .orderBy(SubscriptionPlan::getCreateTime).desc());
    }


    /**
     * 分页查询订阅套餐
     */
    default Page<SubscriptionPlan> selectPlanPage(Page<SubscriptionPlan> page, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(SubscriptionPlan::getPlanName).like(keyword, keyword != null)
                .or(SubscriptionPlan::getDescription).like(keyword, keyword != null)
                .orderBy(SubscriptionPlan::getSortOrder).asc()
                .orderBy(SubscriptionPlan::getCreateTime).desc();

        return paginate(page, query);
    }

    /**
     * 根据排序顺序查询前N个套餐
     */
    default List<SubscriptionPlan> selectTopPlans(int limit) {
        return selectListByQuery(QueryWrapper.create()
                .where(SubscriptionPlan::getEnabled).eq(1)
                .orderBy(SubscriptionPlan::getSortOrder).asc()
                .limit(limit));
    }

    /**
     * 更新套餐启用状态
     */
    default int updateEnabledStatus(Long id, Integer enabled) {
        // 创建仅包含启用状态字段的更新对象
        SubscriptionPlan updatePlan = new SubscriptionPlan();
        updatePlan.setEnabled(enabled);
        return updateByQuery(updatePlan,
                QueryWrapper.create().where(SubscriptionPlan::getId).eq(id));
    }

    /**
     * 根据套餐名称查询套餐列表
     */
    default List<SubscriptionPlan> selectByName(String planName) {
        return selectListByQuery(QueryWrapper.create()
                .where(SubscriptionPlan::getPlanName).like(planName));
    }
}
