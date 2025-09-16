package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.noah.superagent.dao.entity.table.SubscriptionPlanEntityTableDef.SUBSCRIPTION_PLAN_ENTITY;

/**
 * 订阅套餐 Mapper 接口
 */
@Mapper
public interface SubscriptionPlanMapper extends BaseMapper<SubscriptionPlanEntity> {

    /**
     * 查询所有套餐，按ID排序
     */
    default List<SubscriptionPlanEntity> selectAllOrderById() {
        return selectListByQuery(QueryWrapper.create()
                .where(SUBSCRIPTION_PLAN_ENTITY.DELETED.eq(0))
                .orderBy(SUBSCRIPTION_PLAN_ENTITY.ID.asc())
        );
    }
}
