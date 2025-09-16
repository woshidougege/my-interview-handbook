package com.noah.superagent.convert;

import com.noah.superagent.common.dto.request.ScheduledChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ScheduledChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ScheduledChatTaskResponse;
import com.noah.superagent.model.ScheduledChatTaskDTO;

import java.util.List;

/**
 * 定时对话任务Web层转换器
 * 负责 Request <-> DTO <-> Response 转换
 *
 * @author AI Assistant  
 * @since 1.0.0
 */
public abstract class ScheduledChatTaskWebConvert {

    /**
     * 创建请求转换为DTO
     *
     * @param request 创建请求
     * @return 定时对话任务DTO
     */
    public abstract ScheduledChatTaskDTO fromCreateRequest(ScheduledChatTaskCreateRequest request);

    /**
     * 更新请求转换为DTO
     *
     * @param request 更新请求
     * @return 定时对话任务DTO
     */
    public abstract ScheduledChatTaskDTO fromUpdateRequest(ScheduledChatTaskUpdateRequest request);

    /**
     * DTO转换为响应
     *
     * @param dto 定时对话任务DTO
     * @return 定时对话任务响应
     */
    public abstract ScheduledChatTaskResponse toResponse(ScheduledChatTaskDTO dto);

    /**
     * DTO列表转换为响应列表
     *
     * @param dtos 定时对话任务DTO列表
     * @return 定时对话任务响应列表
     */
    public abstract List<ScheduledChatTaskResponse> toResponseList(List<ScheduledChatTaskDTO> dtos);
}