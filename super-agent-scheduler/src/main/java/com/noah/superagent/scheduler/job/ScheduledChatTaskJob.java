package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.common.enums.ChatTaskStatusEnum;
import com.noah.superagent.common.enums.DeletedEnum;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.service.AiService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 定时对话任务处理器
 * <p>
 * 负责执行定时对话任务，根据cron表达式调度任务执行
 *
 * @author noah
 */
@Slf4j
@Component
public class ScheduledChatTaskJob {
    private static final String TASK_NAME = "scheduled-chat-task";
    private static final String CRON_EXPRESSION = "0 * * * * ?"; // 每分钟执行一次

    @Autowired
    private ScheduledChatTaskMapper scheduledChatTaskMapper;

    @Autowired
    private ChatTaskMapper chatTaskMapper;

    @Autowired
    private AiService aiService;

    // RecurringTask 实例
    @Getter
    private final RecurringTask<Void> task;

    public ScheduledChatTaskJob(ScheduledChatTaskMapper scheduledChatTaskMapper,
                                ChatTaskMapper chatTaskMapper,
                                AiService aiService) {
        this.scheduledChatTaskMapper = scheduledChatTaskMapper;
        this.chatTaskMapper = chatTaskMapper;
        this.aiService = aiService;
        // 按照官方文档使用 Tasks.recurring 创建重复任务
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(CRON_EXPRESSION))
                .execute(this::executeTask);
    }

    /**
     * 执行定时任务
     *
     * @param taskInstance     任务实例
     * @param executionContext 执行上下文
     */
    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("开始执行定时对话任务检查 - 任务ID: {}", taskInstance.getId());

        try {
            // 执行定时对话任务检查和处理
            int executedTaskCount = executeScheduledChatTasks();

            // 重新计算所有启用任务的下次执行时间
            recalculateAllEnabledTasksNextExecutionTime();

            long duration = System.currentTimeMillis() - startTime;
            log.info("定时对话任务检查执行完成 - 耗时: {}ms, 执行任务数: {}", duration, executedTaskCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("定时对话任务检查执行失败 - 耗时: {}ms, 错误: {}",
                    duration, e.getMessage(), e);

            // 抛出异常让调度器知道任务失败，可以根据配置进行重试
            throw new RuntimeException("定时对话任务检查执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行需要执行的定时对话任务
     *
     * @return 执行的任务数量
     */
    public int executeScheduledChatTasks() {
        // 查找所有启用且下次执行时间在当前时间之前的任务
        List<ScheduledChatTaskEntity> tasksToExecute = scheduledChatTaskMapper.selectTasksToExecute(new Date());

        int executedCount = 0;
        for (ScheduledChatTaskEntity scheduledTask : tasksToExecute) {
            try {
                // 为定时任务查找或创建对应的对话任务
                ChatTaskEntity chatTask = findOrCreateChatTask(scheduledTask);

                // 执行与大模型的对话
                String response = executeAiTask(scheduledTask);

                // 更新对话任务内容并标记为完成
                updateChatTaskContent(chatTask, response);
                chatTask.setStatus(ChatTaskStatusEnum.COMPLETED);
                chatTask.setUpdateTime(LocalDateTime.now());
                chatTaskMapper.update(chatTask);

                // 保存对话结果到数据库
                saveTaskResult(scheduledTask, response);

                // 对于定时任务，不需要推送前端消息，已通过SSE处理
                // 如果需要记录可以保留日志
                log.info("定时任务完成 - 用户ID: {}, 任务名称: {}", scheduledTask.getUserId(), scheduledTask.getTaskName());

                // 如果是一次性任务(任务类型为0)，执行后禁用任务
                if (scheduledTask.getTaskType() != null && scheduledTask.getTaskType() == 0) {
                    scheduledTask.setStatus(0); // 禁用任务
                    scheduledChatTaskMapper.update(scheduledTask);
                    log.info("一次性定时任务执行完成，已禁用任务 - 任务ID: {}, 任务名称: {}",
                            scheduledTask.getId(), scheduledTask.getTaskName());
                }

                executedCount++;
                log.info("定时对话任务执行成功 - 任务ID: {}, 任务名称: {}", scheduledTask.getId(), scheduledTask.getTaskName());
            } catch (Exception e) {
                log.error("定时对话任务执行失败 - 任务ID: {}, 任务名称: {}, 错误: {}",
                        scheduledTask.getId(), scheduledTask.getTaskName(), e.getMessage(), e);
            }
        }

        return executedCount;
    }

    /**
     * 为定时任务查找或创建对应的对话任务
     *
     * @param scheduledTask 定时任务
     * @return 对应的对话任务
     */
    private ChatTaskEntity findOrCreateChatTask(ScheduledChatTaskEntity scheduledTask) {
        ChatTaskEntity chatTask;
        if (scheduledTask.getChatTaskId() != null) {
            // 如果已有关联的对话任务ID，则查找该任务
            chatTask = chatTaskMapper.selectOneById(scheduledTask.getChatTaskId());
            if (chatTask == null) {
                // 如果找不到关联的对话任务，则创建新的对话任务
                log.warn("找不到关联的对话任务，将创建新的对话任务 - 任务ID: {}", scheduledTask.getChatTaskId());
                chatTask = createNewChatTask(scheduledTask);
            }
        } else {
            // 如果没有关联的对话任务ID，则创建新的对话任务
            chatTask = createNewChatTask(scheduledTask);
        }
        return chatTask;
    }

    /**
     * 创建新的对话任务
     *
     * @param scheduledTask 定时任务
     * @return 新创建的对话任务
     */
    private ChatTaskEntity createNewChatTask(ScheduledChatTaskEntity scheduledTask) {
        ChatTaskEntity chatTask = new ChatTaskEntity();
        chatTask.setWorkspaceId(scheduledTask.getWorkspaceId());
        chatTask.setContextId(UUID.randomUUID().toString());
        chatTask.setTitle(scheduledTask.getTaskName());
        chatTask.setContent("");
        chatTask.setStatus(ChatTaskStatusEnum.IN_PROGRESS);
        chatTask.setDeleted(DeletedEnum.NOT_DELETED);
        chatTask.setCreateBy(scheduledTask.getUserId());
        chatTask.setUpdateBy(scheduledTask.getUserId());
        chatTaskMapper.insert(chatTask);

        // 更新定时任务的关联ID
        scheduledTask.setChatTaskId(chatTask.getId());
        scheduledChatTaskMapper.update(scheduledTask);

        log.info("创建新的对话任务成功 - 定时任务ID: {}, 对话任务ID: {}", scheduledTask.getId(), chatTask.getId());
        return chatTask;
    }

    /**
     * 执行AI对话任务
     *
     * @param scheduledTask 定时任务
     * @return AI响应结果
     */
    private String executeAiTask(ScheduledChatTaskEntity scheduledTask) {
        try {
            return aiService.getAiResponse(
                    scheduledTask.getPrompt(),
                    String.valueOf(scheduledTask.getWorkspaceId()),
                    String.valueOf(scheduledTask.getId())
            );
        } catch (Exception e) {
            log.error("执行AI对话任务失败 - 任务ID: {}, 错误: {}", scheduledTask.getId(), e.getMessage(), e);
            throw new RuntimeException("执行AI对话任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新对话任务内容
     *
     * @param chatTask 对话任务
     * @param content  新的内容
     */
    private void updateChatTaskContent(ChatTaskEntity chatTask, String content) {
        if (chatTask.getContent() == null) {
            chatTask.setContent(content);
        } else {
            chatTask.setContent(chatTask.getContent() + "\n\n" + content);
        }
    }

    /**
     * 保存任务执行结果
     *
     * @param scheduledTask 定时任务
     * @param response      AI响应结果
     */
    private void saveTaskResult(ScheduledChatTaskEntity scheduledTask, String response) {
        // 这里可以保存任务执行结果到专门的结果表中
        // 目前我们只是记录日志
        log.info("保存任务执行结果 - 任务ID: {}, 响应长度: {}", scheduledTask.getId(), response.length());
    }


    /**
     * 重新计算所有启用任务的下次执行时间
     */
    private void recalculateAllEnabledTasksNextExecutionTime() {
        // 查询所有启用的定时任务
        List<ScheduledChatTaskEntity> enabledTasks = scheduledChatTaskMapper.selectByStatus(1);
        log.debug("找到 {} 个启用的定时任务需要重新计算下次执行时间", enabledTasks.size());

        int updatedCount = 0;
        for (ScheduledChatTaskEntity task : enabledTasks) {
            try {
                // 检查任务是否为启用状态且不是一次性任务（一次性任务执行后会被禁用）
                if (task.getStatus() != null && task.getStatus() == 1) {
                    updateNextExecutionTime(task);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("重新计算定时任务下次执行时间失败 - 任务ID: {}, 任务名称: {}, 错误: {}",
                        task.getId(), task.getTaskName(), e.getMessage(), e);
            }
        }

        log.debug("重新计算定时任务下次执行时间完成，共检查 {} 个任务，更新 {} 个任务",
                enabledTasks.size(), updatedCount);
    }

    /**
     * 更新下次执行时间
     *
     * @param task 定时任务
     */
    private void updateNextExecutionTime(ScheduledChatTaskEntity task) {
        try {
            // 使用db-scheduler的cron表达式解析器计算下次执行时间
            com.github.kagkarlsson.scheduler.task.schedule.CronSchedule cronSchedule =
                    Schedules.cron(task.getCronExpression());

            // 获取当前时间
            Instant now = Instant.now();

            // 计算下次执行时间
            Instant nextExecutionTime = cronSchedule.getNextExecutionTime(com.github.kagkarlsson.scheduler.task.ExecutionComplete.simulatedSuccess(now));

            task.setLastExecutionTime(Date.from(now));
            task.setNextExecutionTime(Date.from(nextExecutionTime));
            scheduledChatTaskMapper.update(task);

            log.debug("更新定时任务执行时间成功 - 任务ID: {}, 上次执行时间: {}, 下次执行时间: {}",
                    task.getId(), now, nextExecutionTime);
        } catch (Exception e) {
            log.error("更新定时任务执行时间失败 - 任务ID: {}, 错误: {}", task.getId(), e.getMessage(), e);

            // 出错时设置一个默认的下次执行时间（1小时后）
            Date nextExecutionTime = new Date(System.currentTimeMillis() + 60 * 60 * 1000L);
            task.setLastExecutionTime(new Date());
            task.setNextExecutionTime(nextExecutionTime);
            scheduledChatTaskMapper.update(task);
        }
    }
}