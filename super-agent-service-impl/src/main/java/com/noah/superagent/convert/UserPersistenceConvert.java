package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.UserEntity;
import com.noah.superagent.model.UserDTO;
import org.mapstruct.Mapper;

/**
 * 用户持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserPersistenceConvert extends BasePersistenceConvert<
        UserDTO,                 // 数据传输对象类型
        UserEntity              // 实体类型
> {
}
