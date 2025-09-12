package com.noah.superagent.scheduler.job;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 定时对话任务作业
 * 
 * 每分钟检查一次是否有需要执行的定时对话任务
 * 使用db-scheduler确保高可靠性，避免程序重启导致的调度失效
 *
 * @author System
 * @since 1.0.0
 */
@Component
public class ScheduledChatTaskJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledChatTaskJob.class);
    
    private final ScheduledChatTaskMapper scheduledChatTaskMapper;
    private final ChatTaskMapper chatTaskMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;

    // 任务名称
    private static final String TASK_NAME = "scheduled-chat-task";
    
    // Cron表达式：每分钟执行一次
    private static final String CRON_EXPRESSION = "0 * * * * ?";
    
    // RecurringTask 实例
    private final RecurringTask<Void> task;

    public ScheduledChatTaskJob(ScheduledChatTaskMapper scheduledChatTaskMapper, 
                                ChatTaskMapper chatTaskMapper,
                                ChatWebSocketHandler chatWebSocketHandler) {
        this.scheduledChatTaskMapper = scheduledChatTaskMapper;
        this.chatTaskMapper = chatTaskMapper;
        this.chatWebSocketHandler = chatWebSocketHandler;
        // 按照官方文档使用 Tasks.recurring 创建重复任务
        this.task = Tasks.recurring(TASK_NAME, Schedules.cron(CRON_EXPRESSION))
                .execute(this::executeTask);
    }
    
    public RecurringTask<Void> getTask() {
        return task;
    }

    private void executeTask(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        long startTime = System.currentTimeMillis();
        log.info("开始执行定时对话任务检查 - 任务ID: {}", taskInstance.getId());
        
        try {
            // 执行定时对话任务检查和处理
            int executedTaskCount = executeScheduledChatTasks();
            
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
     * 执行定时对话任务
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
                String response = chatWebSocketHandler.processMessage(scheduledTask.getPrompt(), null);
                
                // 更新对话任务内容
                updateChatTaskContent(chatTask, response);
                
                // 保存对话结果到数据库
                saveTaskResult(scheduledTask, response);
                
                // 更新任务的下次执行时间
                updateNextExecutionTime(scheduledTask);
                
                // 推送结果到前端
                ChatWebSocketHandler.pushMessageToUser(String.valueOf(scheduledTask.getUserId()), 
                    "定时任务通知: 您设置的定时任务[" + scheduledTask.getTaskName() + "]已完成，AI回复内容：" + response);
                
                executedCount++;
                log.info("定时对话任务执行成功 - 任务ID: {}, 任务名称: {}", scheduledTask.getId(), scheduledTask.getTaskName());
            } catch (Exception e) {
                log.error("定时对话任务执行失败 - 任务ID: {}, 任务名称: {}, 错误: {}", 
                        scheduledTask.getId(), scheduledTask.getTaskName(), e.getMessage(), e);
                
                // 推送错误信息到前端
                ChatWebSocketHandler.pushMessageToUser(String.valueOf(scheduledTask.getUserId()), 
                    "定时任务通知: 您设置的定时任务[" + scheduledTask.getTaskName() + "]执行失败：" + e.getMessage());
            }
        }
        
        return executedCount;
    }
    
    /**
     * 查找或创建对话任务
     * @param scheduledTask 定时任务
     * @return 对应的对话任务
     */
    private ChatTaskEntity findOrCreateChatTask(ScheduledChatTaskEntity scheduledTask) {
        // 如果定时任务已经关联了对话任务，则直接使用
        if (scheduledTask.getChatTaskId() != null) {
            ChatTaskEntity chatTask = chatTaskMapper.selectOneById(scheduledTask.getChatTaskId());
            if (chatTask != null) {
                log.info("使用已关联的对话任务，任务ID: {}", chatTask.getId());
                return chatTask;
            }
        }
        
        // 尝试查找现有的对话任务（基于任务名称）
        List<ChatTaskEntity> existingTasks = chatTaskMapper.selectListByQuery(
            com.mybatisflex.core.query.QueryWrapper.create()
                .where(com.noah.superagent.dao.entity.ChatTaskEntity::getWorkspaceId)
                .eq(scheduledTask.getWorkspaceId())
                .and(com.noah.superagent.dao.entity.ChatTaskEntity::getTitle)
                .like("定时任务: " + scheduledTask.getTaskName())
                .orderBy(com.noah.superagent.dao.entity.ChatTaskEntity::getCreateTime).desc()
        );
        
        if (!existingTasks.isEmpty()) {
            ChatTaskEntity existingTask = existingTasks.get(0);
            // 更新定时任务关联的对话任务ID
            scheduledTask.setChatTaskId(existingTask.getId());
            scheduledChatTaskMapper.update(scheduledTask);
            log.info("找到现有的对话任务，任务ID: {}", existingTask.getId());
            return existingTask;
        }
        
        // 创建新的对话任务
        ChatTaskEntity newChatTask = new ChatTaskEntity();
        newChatTask.setWorkspaceId(scheduledTask.getWorkspaceId());
        newChatTask.setContextId(UUID.randomUUID().toString()); // 生成新的会话ID
        newChatTask.setTitle("定时任务: " + scheduledTask.getTaskName());
        newChatTask.setContent(""); // 初始内容为空
        newChatTask.setIsFavorite(com.noah.superagent.common.enums.FavoriteEnum.NOT_FAVORITE);
        newChatTask.setStatus(com.noah.superagent.common.enums.ChatTaskStatusEnum.IN_PROGRESS);
        
        chatTaskMapper.insert(newChatTask);
        
        // 更新定时任务关联的对话任务ID
        scheduledTask.setChatTaskId(newChatTask.getId());
        scheduledChatTaskMapper.update(scheduledTask);
        
        log.info("创建新的对话任务，任务ID: {}", newChatTask.getId());
        
        return newChatTask;
    }
    
    /**
     * 更新对话任务内容
     * @param chatTask 对话任务
     * @param response 响应内容
     */
    private void updateChatTaskContent(ChatTaskEntity chatTask, String response) {
        String newContent = StringUtils.hasText(chatTask.getContent()) ? 
            chatTask.getContent() + "\n---\n" + response : response;
        chatTask.setContent(newContent);
        chatTaskMapper.update(chatTask);
    }
    
    /**
     * 保存任务执行结果
     * @param task 定时任务
     * @param response 响应结果
     */
    private void saveTaskResult(ScheduledChatTaskEntity task, String response) {
        // 这里可以将结果保存到对话任务表或者其他地方
        // 暂时留空，可以根据需要实现
    }
    
    /**
     * 更新下次执行时间
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
            
            log.info("更新定时任务执行时间成功 - 任务ID: {}, 上次执行时间: {}, 下次执行时间: {}", 
                    task.getId(), now, nextExecutionTime);
        } catch (Exception e) {
            log.error("更新定时任务执行时间失败 - 任务ID: {}, 错误: {}", task.getId(), e.getMessage(), e);
            
            // 出错时设置一个默认的下次执行时间（1天后）
            Date nextExecutionTime = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
            task.setLastExecutionTime(new Date());
            task.setNextExecutionTime(nextExecutionTime);
            scheduledChatTaskMapper.update(task);
        }
    }

    /**
     * 获取任务名称
     */
    public static String getJobTaskName() {
        return TASK_NAME;
    }

    /**
     * 获取Cron表达式
     */
    public static String getCronExpression() {
        return CRON_EXPRESSION;
    }
}