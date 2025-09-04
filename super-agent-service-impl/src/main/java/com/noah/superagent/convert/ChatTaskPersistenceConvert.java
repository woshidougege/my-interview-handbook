package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.model.ChatTaskDO;
import org.mapstruct.Mapper;

/**
 * 对话任务持久化层转换器
 * 负责 DO <-> Entity 转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ChatTaskPersistenceConvert extends BasePersistenceConvert<
        ChatTaskDO,              // 领域对象类型
        ChatTaskEntity           // 实体类型
> {
}
