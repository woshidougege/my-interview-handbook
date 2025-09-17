package com.noah.superagent.convert;

import com.noah.superagent.model.ScheduledChatTaskExecutionLogDTO;
import com.noah.superagent.dao.entity.ScheduledChatTaskExecutionLogEntity;
import org.mapstruct.Mapper;

/**
 * 定时对话任务执行日志持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ScheduledChatTaskExecutionLogPersistenceConvert {
    
    /**
     * Entity转换为DTO
     *
     * @param entity Entity对象
     * @return DTO对象
     */
    ScheduledChatTaskExecutionLogDTO fromEntity(ScheduledChatTaskExecutionLogEntity entity);

    /**
     * DTO转换为Entity
     *
     * @param dto DTO对象
     * @return Entity对象
     */
    ScheduledChatTaskExecutionLogEntity toEntity(ScheduledChatTaskExecutionLogDTO dto);
}