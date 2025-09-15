package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.convert.ScheduledChatTaskPersistenceConvert;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.enums.DeletedEnum;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import com.noah.superagent.service.ScheduledChatTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时对话任务服务实现
 * 对应数据库表：t_scheduled_chat_task
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledChatTaskServiceImpl implements ScheduledChatTaskService {

    private final ScheduledChatTaskMapper scheduledChatTaskMapper;
    private final ScheduledChatTaskPersistenceConvert convert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledChatTaskDTO createScheduledChatTask(ScheduledChatTaskDTO taskDTO) {
        log.info("开始创建定时对话任务，任务名称: {}", taskDTO.getTaskName());

        ScheduledChatTaskEntity entity = convert.toEntity(taskDTO);
        int result = scheduledChatTaskMapper.insertSelective(entity);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务创建失败");
        }

        log.info("定时对话任务创建成功，ID: {}", entity.getId());
        return convert.fromEntity(entity);
    }

    @Override
    public ScheduledChatTaskDTO getScheduledChatTaskById(Long id) {
        log.info("查询定时对话任务信息，ID: {}", id);

        ScheduledChatTaskEntity entity = scheduledChatTaskMapper.selectOneById(id);
        if (entity == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        return convert.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledChatTaskDTO updateScheduledChatTask(Long id, ScheduledChatTaskDTO taskDTO) {
        log.info("更新定时对话任务信息，ID: {}", id);

        ScheduledChatTaskEntity existingEntity = scheduledChatTaskMapper.selectOneById(id);
        if (existingEntity == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        // 设置ID并转换为Entity
        taskDTO.setId(id);
        ScheduledChatTaskEntity entity = convert.toEntity(taskDTO);
        
        // 保留系统字段
        entity.setCreateTime(existingEntity.getCreateTime());
        entity.setCreateBy(existingEntity.getCreateBy());

        int result = scheduledChatTaskMapper.update(entity);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务更新失败");
        }

        log.info("定时对话任务更新成功，ID: {}", id);
        return convert.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheduledChatTask(Long id) {
        log.info("删除定时对话任务，ID: {}", id);

        ScheduledChatTaskEntity entity = scheduledChatTaskMapper.selectOneById(id);
        if (entity == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        // 逻辑删除
        entity.setDeleted(DeletedEnum.DELETED);
        int result = scheduledChatTaskMapper.update(entity);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务删除失败");
        }

        log.info("定时对话任务删除成功，ID: {}", id);
    }

    @Override
    public List<ScheduledChatTaskDTO> getScheduledChatTasksByUserId(Long userId) {
        log.info("查询用户定时对话任务列表，用户ID: {}", userId);
        
        return scheduledChatTaskMapper.selectByUserId(userId)
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduledChatTaskDTO> getScheduledChatTasksByWorkspaceId(Long workspaceId) {
        log.info("查询工作空间定时对话任务列表，工作空间ID: {}", workspaceId);
        
        return scheduledChatTaskMapper.selectByWorkspaceId(workspaceId)
                .stream()
                .map(convert::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<ScheduledChatTaskDTO> getScheduledChatTasksPage(Long workspaceId, Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询定时对话任务列表，工作空间ID: {}, 页码: {}, 每页数量: {}, 关键词: {}", workspaceId, pageNum, pageSize, keyword);

        Page<ScheduledChatTaskEntity> page = new Page<>(pageNum, pageSize);
        page = scheduledChatTaskMapper.selectScheduledChatTaskPage(page, workspaceId, keyword);
        
        List<ScheduledChatTaskDTO> records = page.getRecords()
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
    public void enableScheduledChatTask(Long id) {
        log.info("启用定时对话任务，ID: {}", id);
        updateTaskStatus(id, 1);
        log.info("定时对话任务启用成功，ID: {}", id);
    }

    @Override
    public void disableScheduledChatTask(Long id) {
        log.info("禁用定时对话任务，ID: {}", id);
        updateTaskStatus(id, 0);
        log.info("定时对话任务禁用成功，ID: {}", id);
    }

    /**
     * 更新任务状态的私有方法
     */
    private void updateTaskStatus(Long id, Integer status) {
        ScheduledChatTaskEntity entity = scheduledChatTaskMapper.selectOneById(id);
        if (entity == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }
        
        entity.setStatus(status);
        int result = scheduledChatTaskMapper.update(entity);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务状态更新失败");
        }
    }
}
