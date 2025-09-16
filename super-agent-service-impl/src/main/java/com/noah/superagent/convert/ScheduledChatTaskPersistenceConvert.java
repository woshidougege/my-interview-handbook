package com.noah.superagent.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 定时对话任务持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public abstract class ScheduledChatTaskPersistenceConvert implements BasePersistenceConvert<
        ScheduledChatTaskDTO,        // 数据传输对象类型
        ScheduledChatTaskEntity      // 实体类型
> {
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * DTO转换为Entity
     *
     * @param dto DTO对象
     * @return Entity对象
     */
    @Override
    @Mapping(target = "scheduleConfig", source = "scheduleConfig", qualifiedByName = "scheduleConfigToJson")
    public abstract ScheduledChatTaskEntity toEntity(ScheduledChatTaskDTO dto);

    /**
     * Entity转换为DTO
     *
     * @param entity Entity对象
     * @return DTO对象
     */
    @Override
    @Mapping(target = "scheduleConfig", source = "scheduleConfig", qualifiedByName = "jsonToScheduleConfig")
    public abstract ScheduledChatTaskDTO fromEntity(ScheduledChatTaskEntity entity);
    
    /**
     * ScheduleConfig对象转JSON字符串
     *
     * @param scheduleConfig ScheduleConfig对象
     * @return JSON字符串
     */
    @Named("scheduleConfigToJson")
    String scheduleConfigToJson(com.noah.superagent.common.dto.ScheduleConfig scheduleConfig) {
        if (scheduleConfig == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(scheduleConfig);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ScheduleConfig to JSON", e);
        }
    }
    
    /**
     * JSON字符串转ScheduleConfig对象
     *
     * @param json JSON字符串
     * @return ScheduleConfig对象
     */
    @Named("jsonToScheduleConfig")
    com.noah.superagent.common.dto.ScheduleConfig jsonToScheduleConfig(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, com.noah.superagent.common.dto.ScheduleConfig.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to ScheduleConfig", e);
        }
    }
}