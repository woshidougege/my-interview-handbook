package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import org.mapstruct.Mapper;

/**
 * 定时对话任务持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ScheduledChatTaskPersistenceConvert extends BasePersistenceConvert<
        ScheduledChatTaskDTO,        // 数据传输对象类型
        ScheduledChatTaskEntity      // 实体类型
> {
}
