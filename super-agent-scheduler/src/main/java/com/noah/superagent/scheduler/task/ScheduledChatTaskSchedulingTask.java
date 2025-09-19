package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.common.event.ScheduledChatTaskCreatedEvent;
import com.noah.superagent.common.event.ScheduledChatTaskDeletedEvent;
import com.noah.superagent.common.event.ScheduledChatTaskUpdatedEvent;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.scheduler.task.ScheduledChatExecutionTask.ChatTaskData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;

/**
 * 定时聊天任务调度事件监听器
 * <p>
 * 监听定时聊天任务的创建、更新、删除事件，动态管理调度任务
 * 采用事件驱动架构，精确控制任务执行时间，避免轮询数据库
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledChatTaskSchedulingTask {

    private final Scheduler scheduler;
    private final ScheduledChatExecutionTask chatExecutionTask;
    private final ScheduledChatTaskMapper scheduledChatTaskMapper;

    /**
     * 监听定时聊天任务创建事件
     */
    @EventListener
    public void handleTaskCreated(ScheduledChatTaskCreatedEvent event) {
        log.info("【定时聊天调度】收到任务创建事件 - taskId: {}, taskName: {}, firstExecutionTime: {}", 
                event.getTaskId(), event.getTaskName(), event.getFirstExecutionTime());

        try {
            scheduleTask(
                event.getTaskId(),
                event.getUserId(),
                event.getWorkspaceId(),
                event.getTaskName(),
                event.getTaskType(),
                event.getFirstExecutionTime(),
                event.getCronExpression()
            );

            log.info("【定时聊天调度】任务创建调度成功 - taskId: {}", event.getTaskId());

        } catch (Exception e) {
            log.error("【定时聊天调度】任务创建调度失败 - taskId: {}, error: {}", 
                    event.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("定时聊天任务调度失败", e);
        }
    }

    /**
     * 监听定时聊天任务更新事件
     */
    @EventListener
    public void handleTaskUpdated(ScheduledChatTaskUpdatedEvent event) {
        log.info("【定时聊天调度】收到任务更新事件 - taskId: {}, taskName: {}, nextExecutionTime: {}", 
                event.getTaskId(), event.getTaskName(), event.getNextExecutionTime());

        try {
            // 先取消现有任务
            cancelTask(event.getTaskId());

            // 重新安排任务
            scheduleTask(
                event.getTaskId(),
                event.getUserId(),
                event.getWorkspaceId(),
                event.getTaskName(),
                event.getTaskType(),
                event.getNextExecutionTime(),
                event.getCronExpression()
            );

            log.info("【定时聊天调度】任务更新调度成功 - taskId: {}", event.getTaskId());

        } catch (Exception e) {
            log.error("【定时聊天调度】任务更新调度失败 - taskId: {}, error: {}", 
                    event.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("定时聊天任务重新调度失败", e);
        }
    }

    /**
     * 监听定时聊天任务删除事件
     */
    @EventListener
    public void handleTaskDeleted(ScheduledChatTaskDeletedEvent event) {
        log.info("【定时聊天调度】收到任务删除事件 - taskId: {}", event.getTaskId());

        try {
            cancelTask(event.getTaskId());
            log.info("【定时聊天调度】任务删除调度成功 - taskId: {}", event.getTaskId());

        } catch (Exception e) {
            log.error("【定时聊天调度】任务删除调度失败 - taskId: {}, error: {}", 
                    event.getTaskId(), e.getMessage(), e);
            // 删除失败不抛异常，避免影响业务流程
        }
    }

    /**
     * 调度任务
     */
    private void scheduleTask(Long taskId, 
                            Long userId, 
                            Long workspaceId,
                            String taskName,
                            Integer taskType,
                            Instant executionTime, 
                            String cronExpression) {
        
        // 创建任务数据
        ChatTaskData taskData = new ChatTaskData();
        taskData.setTaskId(taskId);
        taskData.setUserId(userId);
        taskData.setWorkspaceId(workspaceId);
        taskData.setTaskName(taskName);
        taskData.setTaskType(taskType);
        taskData.setCronExpression(cronExpression);

        // 创建任务实例
        TaskInstance<ChatTaskData> taskInstance = chatExecutionTask.getTask()
                .instance(generateTaskInstanceId(taskId), taskData);

        // 安排执行
        scheduler.schedule(taskInstance, executionTime);

        log.info("【定时聊天调度】任务安排成功 - taskId: {}, executionTime: {}", taskId, executionTime);

        // 更新数据库中的下次执行时间
        updateNextExecutionTime(taskId, executionTime);
    }

    /**
     * 取消任务
     */
    private void cancelTask(Long taskId) {
        String taskInstanceId = generateTaskInstanceId(taskId);
        scheduler.cancel(chatExecutionTask.getTask().instance(taskInstanceId));
        log.info("【定时聊天调度】任务取消成功 - taskId: {}", taskId);
    }

    /**
     * 生成任务实例ID
     */
    private String generateTaskInstanceId(Long taskId) {
        return "chat-task-" + taskId;
    }

    /**
     * 更新数据库中的下次执行时间
     */
    private void updateNextExecutionTime(Long taskId, Instant executionTime) {
        try {
            ScheduledChatTaskEntity entity = new ScheduledChatTaskEntity();
            entity.setId(taskId);
            entity.setNextExecutionTime(Date.from(executionTime));
            scheduledChatTaskMapper.update(entity);
        } catch (Exception e) {
            log.warn("【定时聊天调度】更新下次执行时间失败 - taskId: {}", taskId, e);
        }
    }

    /**
     * 处理重复任务的下次调度
     * <p>
     * 当重复任务执行完成后，根据CRON表达式计算并安排下次执行
     */
    public void scheduleNextExecution(Long taskId, String cronExpression) {
        if (!StringUtils.hasText(cronExpression)) {
            log.warn("【定时聊天调度】重复任务缺少CRON表达式 - taskId: {}", taskId);
            return;
        }

        try {
            // 查询任务详情
            ScheduledChatTaskEntity scheduledTask = scheduledChatTaskMapper.selectOneById(taskId);
            if (scheduledTask == null || scheduledTask.getStatus() != 1) {
                log.warn("【定时聊天调度】任务不存在或已禁用，跳过下次调度 - taskId: {}", taskId);
                return;
            }

            // 使用CRON表达式计算下次执行时间
            var cronSchedule = Schedules.cron(cronExpression);
            Instant now = Instant.now();
            Instant nextExecutionTime = cronSchedule.getNextExecutionTime(
                com.github.kagkarlsson.scheduler.task.ExecutionComplete.simulatedSuccess(now)
            );

            // 创建任务数据
            ChatTaskData taskData = new ChatTaskData();
            taskData.setTaskId(taskId);
            taskData.setUserId(scheduledTask.getUserId());
            taskData.setWorkspaceId(scheduledTask.getWorkspaceId());
            taskData.setTaskName(scheduledTask.getTaskName());
            taskData.setTaskType(scheduledTask.getTaskType());
            taskData.setCronExpression(cronExpression);

            // 创建并安排下次执行
            TaskInstance<ChatTaskData> taskInstance = chatExecutionTask.getTask()
                    .instance(generateTaskInstanceId(taskId), taskData);
            
            scheduler.schedule(taskInstance, nextExecutionTime);

            // 更新数据库
            updateNextExecutionTime(taskId, nextExecutionTime);

            log.info("【定时聊天调度】重复任务下次执行安排成功 - taskId: {}, nextExecutionTime: {}", 
                    taskId, nextExecutionTime);

        } catch (Exception e) {
            log.error("【定时聊天调度】重复任务下次执行安排失败 - taskId: {}, error: {}", 
                    taskId, e.getMessage(), e);
        }
    }
}
