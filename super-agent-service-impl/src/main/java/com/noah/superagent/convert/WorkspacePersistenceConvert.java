package com.noah.superagent.convert;

import com.noah.superagent.dao.entity.WorkspaceEntity;
import com.noah.superagent.model.WorkspaceDO;
import org.mapstruct.Mapper;

/**
 * 工作空间持久化层转换器
 * 负责 DO <-> Entity 转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface WorkspacePersistenceConvert extends BasePersistenceConvert<
        WorkspaceDO,             // 领域对象类型
        WorkspaceEntity          // 实体类型
> {
}
