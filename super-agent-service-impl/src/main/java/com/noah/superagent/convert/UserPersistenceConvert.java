package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.UserEntity;
import com.noah.superagent.model.UserDO;
import org.mapstruct.Mapper;

/**
 * 用户持久化层转换器
 * 负责 DO <-> Entity 转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserPersistenceConvert extends BasePersistenceConvert<
        UserDO,                  // 领域对象类型
        UserEntity              // 实体类型
> {
}
