package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.common.event.ScheduledChatTaskCreatedEvent;
import com.noah.superagent.common.event.ScheduledChatTaskDeletedEvent;
import com.noah.superagent.common.event.ScheduledChatTaskUpdatedEvent;
import com.noah.superagent.convert.ScheduledChatTaskPersistenceConvert;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import com.noah.superagent.service.ScheduledChatTaskService;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.noah.superagent.common.exception.BusinessException;
import com.noah.superagent.common.enums.EnabledEnum;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时对话任务服务实现
 * 对应数据库表：t_scheduled_chat_task
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledChatTaskServiceImpl implements ScheduledChatTaskService {

    private final ScheduledChatTaskMapper scheduledChatTaskMapper;
    private final ScheduledChatTaskPersistenceConvert convert;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledChatTaskDTO createScheduledChatTask(ScheduledChatTaskDTO taskDTO) {
        log.info("开始创建定时对话任务，任务名称: {}", taskDTO.getTaskName());

        ScheduledChatTaskEntity entity = convert.toEntity(taskDTO);

        // 保存定时对话任务
        int result = scheduledChatTaskMapper.insertSelective(entity);
        BusinessException.throwIf(result <= 0, "定时对话任务创建失败");

        log.info("定时对话任务创建成功，ID: {}", entity.getId());

        // 如果任务已启用，发布创建事件触发调度
        if (entity.getStatus() != null && entity.getStatus().equals(EnabledEnum.ENABLED.getCode())) {
            publishTaskCreatedEvent(entity);
        }

        return convert.fromEntity(entity);
    }

    @Override
    public ScheduledChatTaskDTO getScheduledChatTaskById(Long id) {
        log.info("查询定时对话任务信息，ID: {}", id);

        ScheduledChatTaskEntity entity = scheduledChatTaskMapper.selectOneById(id);
        BusinessException.throwIf(entity == null, "定时对话任务不存在: " + id);

        return convert.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledChatTaskDTO updateScheduledChatTask(Long id, ScheduledChatTaskDTO taskDTO) {
        log.info("更新定时对话任务信息，ID: {}", id);

        ScheduledChatTaskEntity existingEntity = scheduledChatTaskMapper.selectOneById(id);
        BusinessException.throwIf(existingEntity == null, "定时对话任务不存在: " + id);

        // 设置ID并转换为Entity
        taskDTO.setId(id);
        ScheduledChatTaskEntity entity = convert.toEntity(taskDTO);
        
        // 保留系统字段
        entity.setCreateTime(existingEntity.getCreateTime());
        entity.setCreateBy(existingEntity.getCreateBy());

        int result = scheduledChatTaskMapper.update(entity);
        BusinessException.throwIf(result <= 0, "定时对话任务更新失败");

        log.info("定时对话任务更新成功，ID: {}", id);

        // 如果任务状态为启用，发布更新事件重新调度
        if (entity.getStatus() != null && entity.getStatus().equals(EnabledEnum.ENABLED.getCode())) {
            publishTaskUpdatedEvent(entity);
        } else {
            // 如果任务被禁用，发布删除事件取消调度
            publishTaskDeletedEvent(id);
        }

        return convert.fromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheduledChatTask(Long id) {
        log.info("删除定时对话任务，ID: {}", id);

        ScheduledChatTaskEntity entity = scheduledChatTaskMapper.selectOneById(id);
        BusinessException.throwIf(entity == null, "定时对话任务不存在: " + id);

        int result = scheduledChatTaskMapper.deleteById(id);
        BusinessException.throwIf(result <= 0, "定时对话任务删除失败");

        // 发布删除事件取消调度
        publishTaskDeletedEvent(id);

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
    @Transactional(rollbackFor = Exception.class)
    public void enableScheduledChatTask(Long id) {
        log.info("启用定时对话任务，ID: {}", id);
        
        ScheduledChatTaskEntity task = scheduledChatTaskMapper.selectOneById(id);
        BusinessException.throwIf(task == null, "定时对话任务不存在: " + id);
        
        task.setStatus(EnabledEnum.ENABLED.getCode()); // 启用状态
        int result = scheduledChatTaskMapper.update(task);
        BusinessException.throwIf(result <= 0, "启用定时对话任务失败");

        // 发布启用事件（相当于创建调度）
        publishTaskCreatedEvent(task);
        
        log.info("定时对话任务启用成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableScheduledChatTask(Long id) {
        log.info("禁用定时对话任务，ID: {}", id);
        
        ScheduledChatTaskEntity task = scheduledChatTaskMapper.selectOneById(id);
        BusinessException.throwIf(task == null, "定时对话任务不存在: " + id);
        
        task.setStatus(EnabledEnum.DISABLED.getCode()); // 禁用状态
        int result = scheduledChatTaskMapper.update(task);
        BusinessException.throwIf(result <= 0, "禁用定时对话任务失败");

        // 发布删除事件取消调度
        publishTaskDeletedEvent(id);
        
        log.info("定时对话任务禁用成功，ID: {}", id);
    }

    /**
     * 发布任务创建事件
     */
    private void publishTaskCreatedEvent(ScheduledChatTaskEntity entity) {
        if (!StringUtils.hasText(entity.getCronExpression())) {
            log.warn("定时任务缺少CRON表达式，跳过调度 - taskId: {}", entity.getId());
            return;
        }

        try {
            // 计算首次执行时间
            Instant firstExecutionTime = calculateFirstExecutionTime(entity);

            ScheduledChatTaskCreatedEvent event = new ScheduledChatTaskCreatedEvent(
                this,
                entity.getId(),
                entity.getUserId(),
                entity.getWorkspaceId(),
                entity.getTaskName(),
                entity.getPrompt(),
                entity.getTaskType(),
                firstExecutionTime,
                entity.getCronExpression()
            );

            eventPublisher.publishEvent(event);
            log.info("发布定时聊天任务创建事件 - taskId: {}", entity.getId());

        } catch (Exception e) {
            log.error("发布定时聊天任务创建事件失败 - taskId: {}, error: {}", entity.getId(), e.getMessage(), e);
        }
    }

    /**
     * 发布任务更新事件
     */
    private void publishTaskUpdatedEvent(ScheduledChatTaskEntity entity) {
        if (!StringUtils.hasText(entity.getCronExpression())) {
            log.warn("定时任务缺少CRON表达式，跳过调度 - taskId: {}", entity.getId());
            return;
        }

        try {
            // 计算下次执行时间
            Instant nextExecutionTime = calculateFirstExecutionTime(entity);

            ScheduledChatTaskUpdatedEvent event = new ScheduledChatTaskUpdatedEvent(
                this,
                entity.getId(),
                entity.getUserId(),
                entity.getWorkspaceId(),
                entity.getTaskName(),
                entity.getPrompt(),
                entity.getTaskType(),
                nextExecutionTime,
                entity.getCronExpression()
            );

            eventPublisher.publishEvent(event);
            log.info("发布定时聊天任务更新事件 - taskId: {}", entity.getId());

        } catch (Exception e) {
            log.error("发布定时聊天任务更新事件失败 - taskId: {}, error: {}", entity.getId(), e.getMessage(), e);
        }
    }

    /**
     * 发布任务删除事件
     */
    private void publishTaskDeletedEvent(Long taskId) {
        try {
            ScheduledChatTaskDeletedEvent event = new ScheduledChatTaskDeletedEvent(this, taskId);
            eventPublisher.publishEvent(event);
            log.info("发布定时聊天任务删除事件 - taskId: {}", taskId);

        } catch (Exception e) {
            log.error("发布定时聊天任务删除事件失败 - taskId: {}, error: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 计算首次执行时间
     * 这里使用简单的逻辑，调度模块会根据CRON表达式精确计算
     */
    private Instant calculateFirstExecutionTime(ScheduledChatTaskEntity entity) {
        // 对于一次性任务，使用下次执行时间
        if (entity.getTaskType() == 0 && entity.getNextExecutionTime() != null) {
            return entity.getNextExecutionTime().toInstant();
        }

        // 对于重复任务或没有指定时间的，默认1分钟后执行（让调度器精确计算）
        return Instant.now().plusSeconds(60);
    }
}
