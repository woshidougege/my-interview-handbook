package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.WorkspaceEntity;
import com.noah.superagent.model.WorkspaceDTO;
import org.mapstruct.Mapper;

/**
 * 工作空间持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface WorkspacePersistenceConvert extends BasePersistenceConvert<
        WorkspaceDTO,            // 数据传输对象类型
        WorkspaceEntity          // 实体类型
> {
}
