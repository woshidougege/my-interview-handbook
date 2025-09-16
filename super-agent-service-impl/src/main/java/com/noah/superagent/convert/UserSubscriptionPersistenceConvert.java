package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import com.noah.superagent.model.UserSubscriptionDTO;
import org.mapstruct.Mapper;

/**
 * 用户订阅持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserSubscriptionPersistenceConvert extends BasePersistenceConvert<
        UserSubscriptionDTO,        // 数据传输对象类型
        UserSubscriptionEntity      // 实体类型
> {
}
