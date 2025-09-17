package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.convert.ScheduledChatTaskExecutionLogPersistenceConvert;
import com.noah.superagent.dao.entity.ScheduledChatTaskExecutionLogEntity;
import com.noah.superagent.dao.mapper.ScheduledChatTaskExecutionLogMapper;
import com.noah.superagent.model.ScheduledChatTaskExecutionLogDTO;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.service.ScheduledChatTaskExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时对话任务执行日志服务实现类
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledChatTaskExecutionLogServiceImpl implements ScheduledChatTaskExecutionLogService {

    private final ScheduledChatTaskExecutionLogMapper scheduledChatTaskExecutionLogMapper;
    private final ScheduledChatTaskExecutionLogPersistenceConvert convert;

    @Override
    public List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByTaskId(Long taskId) {
        log.info("查询定时对话任务执行日志列表，任务ID: {}", taskId);

        return scheduledChatTaskExecutionLogMapper.selectByTaskId(taskId)
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByChatTaskId(Long chatTaskId) {
        log.info("查询定时对话任务执行日志列表，对话任务ID: {}", chatTaskId);

        return scheduledChatTaskExecutionLogMapper.selectByChatTaskId(chatTaskId)
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduledChatTaskExecutionLogDTO> getExecutionLogsByTaskName(String taskName) {
        log.info("查询定时对话任务执行日志列表，任务名称: {}", taskName);

        return scheduledChatTaskExecutionLogMapper.selectByTaskName(taskName)
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ScheduledChatTaskExecutionLogDTO getExecutionLogById(Long id) {
        log.info("查询定时对话任务执行日志详情，ID: {}", id);

        ScheduledChatTaskExecutionLogEntity entity = scheduledChatTaskExecutionLogMapper.selectOneById(id);
        return convert.fromEntity(entity);
    }

    @Override
    public PageResponse<ScheduledChatTaskExecutionLogDTO> getExecutionLogsPage(String taskName, Integer pageNum, Integer pageSize) {
        log.info("分页查询定时对话任务执行日志列表，任务名称: {}, 页码: {}, 每页数量: {}", taskName, pageNum, pageSize);

        Page<ScheduledChatTaskExecutionLogEntity> page = new Page<>(pageNum, pageSize);
        page = scheduledChatTaskExecutionLogMapper.selectExecutionLogPage(page, taskName);

        List<ScheduledChatTaskExecutionLogDTO> records = page.getRecords()
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                records,
                page.getTotalRow(),
                (int) page.getPageNumber(),
                (int) page.getPageSize()
        );
    }

    @Override
    public PageResponse<ScheduledChatTaskExecutionLogDTO> getExecutionLogsPageByChatTaskId(Long chatTaskId, Integer pageNum, Integer pageSize) {
        log.info("根据对话任务ID分页查询定时对话任务执行日志列表，对话任务ID: {}, 页码: {}, 每页数量: {}", chatTaskId, pageNum, pageSize);

        Page<ScheduledChatTaskExecutionLogEntity> page = new Page<>(pageNum, pageSize);
        page = scheduledChatTaskExecutionLogMapper.selectExecutionLogPageByChatTaskId(page, chatTaskId);

        List<ScheduledChatTaskExecutionLogDTO> records = page.getRecords()
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                records,
                page.getTotalRow(),
                (int) page.getPageNumber(),
                (int) page.getPageSize()
        );
    }
}