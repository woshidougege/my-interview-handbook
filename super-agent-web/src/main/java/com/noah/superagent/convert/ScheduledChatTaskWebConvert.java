package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.ScheduledChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ScheduledChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ScheduledChatTaskResponse;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 定时对话任务Web层转换器
 * 实现DTO与Entity之间的相互转换
 *
 * @author System
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ScheduledChatTaskWebConvert {

    /**
     * 创建请求转换为实体
     *
     * @param request 创建请求
     * @return 定时对话任务实体
     */
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "createBy", ignore = true),
            @Mapping(target = "updateBy", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "lastExecutionTime", ignore = true),
            @Mapping(target = "nextExecutionTime", ignore = true)
    })
    ScheduledChatTaskEntity fromCreateRequest(ScheduledChatTaskCreateRequest request);

    /**
     * 更新请求转换为实体
     *
     * @param request 更新请求
     * @return 定时对话任务实体
     */
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "workspaceId", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "createBy", ignore = true),
            @Mapping(target = "updateBy", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "lastExecutionTime", ignore = true),
            @Mapping(target = "nextExecutionTime", ignore = true)
    })
    ScheduledChatTaskEntity fromUpdateRequest(ScheduledChatTaskUpdateRequest request);

    /**
     * 实体转换为响应
     *
     * @param entity 定时对话任务实体
     * @return 定时对话任务响应
     */
    ScheduledChatTaskResponse toResponse(ScheduledChatTaskEntity entity);

    /**
     * 实体列表转换为响应列表
     *
     * @param entities 定时对话任务实体列表
     * @return 定时对话任务响应列表
     */
    List<ScheduledChatTaskResponse> toResponseList(List<ScheduledChatTaskEntity> entities);
}