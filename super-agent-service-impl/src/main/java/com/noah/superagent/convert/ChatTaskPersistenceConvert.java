package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.model.ChatTaskDTO;
import org.mapstruct.Mapper;

/**
 * 对话任务持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ChatTaskPersistenceConvert extends BasePersistenceConvert<
        ChatTaskDTO,             // 数据传输对象类型
        ChatTaskEntity           // 实体类型
> {
}
