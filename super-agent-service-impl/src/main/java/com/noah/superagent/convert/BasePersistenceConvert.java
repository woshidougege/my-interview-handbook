package com.noah.superagent.convert;

import java.util.List;

/**
 * 持久化层转换器基接口
 * 负责 DTO <-> Entity 转换
 * 
 * @param <DTO> 数据传输对象类型
 * @param <Entity> 实体类型
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface BasePersistenceConvert<DTO, Entity> {

    // ==================== DTO <-> Entity ====================
    
    /**
     * 数据传输对象转实体
     */
    Entity toEntity(DTO dataTransferObject);

    /**
     * 实体转数据传输对象
     */
    DTO fromEntity(Entity entity);

    /**
     * 实体列表转数据传输对象列表
     */
    List<DTO> fromEntityList(List<Entity> entities);

    /**
     * 数据传输对象列表转实体列表
     */
    List<Entity> toEntityList(List<DTO> dataTransferObjects);
}
