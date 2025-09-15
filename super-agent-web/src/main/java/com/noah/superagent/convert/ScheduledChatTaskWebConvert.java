package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.ScheduledChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ScheduledChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ScheduledChatTaskResponse;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 定时对话任务Web层转换器
 * 负责 Request <-> DTO <-> Response 转换
 *
 * @author AI Assistant  
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ScheduledChatTaskWebConvert {

    /**
     * 创建请求转换为DTO
     *
     * @param request 创建请求
     * @return 定时对话任务DTO
     */
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "createBy", ignore = true),
            @Mapping(target = "updateBy", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "lastExecutionTime", ignore = true),
            @Mapping(target = "nextExecutionTime", ignore = true),
            @Mapping(target = "chatTaskId", ignore = true)
    })
    ScheduledChatTaskDTO fromCreateRequest(ScheduledChatTaskCreateRequest request);

    /**
     * 更新请求转换为DTO
     *
     * @param request 更新请求
     * @return 定时对话任务DTO
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
            @Mapping(target = "nextExecutionTime", ignore = true),
            @Mapping(target = "chatTaskId", ignore = true)
    })
    ScheduledChatTaskDTO fromUpdateRequest(ScheduledChatTaskUpdateRequest request);

    /**
     * DTO转换为响应
     *
     * @param dto 定时对话任务DTO
     * @return 定时对话任务响应
     */
    ScheduledChatTaskResponse toResponse(ScheduledChatTaskDTO dto);

    /**
     * DTO列表转换为响应列表
     *
     * @param dtos 定时对话任务DTO列表
     * @return 定时对话任务响应列表
     */
    List<ScheduledChatTaskResponse> toResponseList(List<ScheduledChatTaskDTO> dtos);
}