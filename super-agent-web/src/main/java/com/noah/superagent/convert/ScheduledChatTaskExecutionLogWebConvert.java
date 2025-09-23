package com.noah.superagent.convert;

import com.noah.superagent.model.ScheduledChatTaskExecutionLogDTO;
import com.noah.superagent.common.dto.response.ScheduledChatTaskExecutionLogResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 定时对话任务执行日志Web层转换器
 * 实现DTO与Response之间的相互转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ScheduledChatTaskExecutionLogWebConvert {

    /**
     * DTO转换为响应
     *
     * @param dto 定时对话任务执行日志DTO
     * @return 定时对话任务执行日志响应
     */
    ScheduledChatTaskExecutionLogResponse toResponse(ScheduledChatTaskExecutionLogDTO dto);

    /**
     * DTO列表转换为响应列表
     *
     * @param dtos 定时对话任务执行日志DTO列表
     * @return 定时对话任务执行日志响应列表
     */
    List<ScheduledChatTaskExecutionLogResponse> toResponseList(List<ScheduledChatTaskExecutionLogDTO> dtos);
}