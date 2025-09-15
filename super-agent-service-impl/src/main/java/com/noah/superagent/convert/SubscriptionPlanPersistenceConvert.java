package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import com.noah.superagent.model.SubscriptionPlanDTO;
import org.mapstruct.Mapper;

/**
 * 订阅套餐持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface SubscriptionPlanPersistenceConvert extends BasePersistenceConvert<
        SubscriptionPlanDTO,        // 数据传输对象类型
        SubscriptionPlanEntity      // 实体类型
> {
    // MapStruct 会自动处理 PlanFeature 和 PlanFeatureDTO 之间的映射
    // 因为它们有相同的字段名
}
