package com.noah.superagent.service;

import com.noah.superagent.model.ScheduledChatTaskExecutionLogDTO;
import com.noah.superagent.common.dto.response.PageResponse;

import java.util.List;

/**
 * 定时对话任务执行日志服务接口
 *
 * @author System
 * @since 1.0.0
 */
public interface ScheduledChatTaskExecutionLogService {

    /**
     * 根据任务ID获取执行日志列表
     *
     * @param taskId 定时任务ID
     * @return 执行日志列表
     */
    List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByTaskId(Long taskId);

    /**
     * 根据对话任务ID获取执行日志列表
     *
     * @param chatTaskId 对话任务ID
     * @return 执行日志列表
     */
    List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByChatTaskId(Long chatTaskId);

    /**
     * 根据任务名称获取执行日志列表
     *
     * @param taskName 任务名称
     * @return 执行日志列表
     */
    List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByTaskName(String taskName);

    /**
     * 根据执行日志ID获取执行日志详情
     *
     * @param id 执行日志ID
     * @return 执行日志详情
     */
    ScheduledChatTaskExecutionLogDTO getExecutionLogById(Long id);

    /**
     * 分页查询执行日志
     *
     * @param taskName 任务名称
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResponse<ScheduledChatTaskExecutionLogDTO> getExecutionLogsPage(String taskName, Integer pageNum, Integer pageSize);

    /**
     * 根据对话任务ID分页查询执行日志
     *
     * @param chatTaskId 对话任务ID
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 分页结果
     */
    PageResponse<ScheduledChatTaskExecutionLogDTO> getExecutionLogsPageByChatTaskId(Long chatTaskId, Integer pageNum, Integer pageSize);
}