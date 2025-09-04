package com.noah.superagent.convert;

import java.util.List;

/**
 * 持久化层转换器基接口
 * 负责 DO <-> Entity 转换
 * 
 * @param <DO> 领域对象类型
 * @param <Entity> 实体类型
 *
 * @author System
 * @since 1.0.0
 */
public interface BasePersistenceConvert<DO, Entity> {

    // ==================== DO <-> Entity ====================
    
    /**
     * 领域对象转实体
     */
    Entity toEntity(DO domainObject);

    /**
     * 实体转领域对象
     */
    DO fromEntity(Entity entity);

    /**
     * 实体列表转领域对象列表
     */
    List<DO> fromEntityList(List<Entity> entities);

    /**
     * 领域对象列表转实体列表
     */
    List<Entity> toEntityList(List<DO> domainObjects);
}
