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
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
        entity.setDeleted(DeletedEnum.NOT_DELETED);

        // 如果任务已启用且未设置下次执行时间，则计算并设置首次的下次执行时间
        if (entity.getStatus() != null && entity.getStatus() == 1 && entity.getNextExecutionTime() == null) {
            try {
                // 使用反射方式调用Cron表达式解析，避免直接依赖db-scheduler包
                Class<?> schedulesClass = Class.forName("com.github.kagkarlsson.scheduler.task.schedule.Schedules");
                Object cronSchedule = schedulesClass.getMethod("cron", String.class)
                    .invoke(null, entity.getCronExpression());
                
                Class<?> cronScheduleClass = cronSchedule.getClass();
                
                // 获取当前时间
                Instant now = Instant.now();
                
                // 创建ExecutionComplete模拟对象
                Class<?> executionCompleteClass = Class.forName("com.github.kagkarlsson.scheduler.task.ExecutionComplete");
                Object executionComplete = executionCompleteClass.getMethod("simulatedSuccess", Instant.class)
                    .invoke(null, now);
                
                // 计算下次执行时间
                Instant nextExecutionTime = (Instant) cronScheduleClass.getMethod("getNextExecutionTime", executionCompleteClass)
                    .invoke(cronSchedule, executionComplete);
                
                entity.setNextExecutionTime(Date.from(nextExecutionTime));
                
                log.info("设置定时任务首次执行时间，任务名称: {}, 下次执行时间: {}", entity.getTaskName(), nextExecutionTime);
            } catch (Exception e) {
                log.warn("计算定时任务下次执行时间失败，任务名称: {}, 错误: {}", entity.getTaskName(), e.getMessage());
                // 如果计算失败，设置为1小时后执行
                entity.setNextExecutionTime(new Date(System.currentTimeMillis() + 60 * 60 * 1000L));
            }
        }

        // 保存定时对话任务
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
        
        ScheduledChatTaskEntity task = scheduledChatTaskMapper.selectOneById(id);
        if (task == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }
        
        task.setStatus(1); // 启用状态
        
        // 计算下次执行时间
        try {
            Class<?> schedulesClass = Class.forName("com.github.kagkarlsson.scheduler.task.schedule.Schedules");
            Object cronSchedule = schedulesClass.getMethod("cron", String.class)
                .invoke(null, task.getCronExpression());
            
            Class<?> cronScheduleClass = cronSchedule.getClass();
            
            // 获取当前时间
            Instant now = Instant.now();
            
            // 创建ExecutionComplete模拟对象
            Class<?> executionCompleteClass = Class.forName("com.github.kagkarlsson.scheduler.task.ExecutionComplete");
            Object executionComplete = executionCompleteClass.getMethod("simulatedSuccess", Instant.class)
                .invoke(null, now);
            
            // 计算下次执行时间
            Instant nextExecutionTime = (Instant) cronScheduleClass.getMethod("getNextExecutionTime", executionCompleteClass)
                .invoke(cronSchedule, executionComplete);
            
            task.setNextExecutionTime(Date.from(nextExecutionTime));
            
            log.info("设置定时任务下次执行时间，任务ID: {}, 下次执行时间: {}", id, nextExecutionTime);
        } catch (Exception e) {
            log.warn("计算定时任务下次执行时间失败，任务ID: {}, 错误: {}", id, e.getMessage());
            // 如果计算失败，设置为1小时后执行
            task.setNextExecutionTime(new Date(System.currentTimeMillis() + 60 * 60 * 1000L));
        }
        
        int result = scheduledChatTaskMapper.update(task);
        if (result <= 0) {
            throw new RuntimeException("启用定时对话任务失败");
        }
        
        log.info("定时对话任务启用成功，ID: {}", id);
    }

    @Override
    public void disableScheduledChatTask(Long id) {
        log.info("禁用定时对话任务，ID: {}", id);
        
        ScheduledChatTaskEntity task = scheduledChatTaskMapper.selectOneById(id);
        if (task == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }
        
        task.setStatus(0); // 禁用状态
        int result = scheduledChatTaskMapper.update(task);
        if (result <= 0) {
            throw new RuntimeException("禁用定时对话任务失败");
        }
        
        log.info("定时对话任务禁用成功，ID: {}", id);
    }
}
