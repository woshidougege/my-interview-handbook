package com.noah.superagent.scheduler.task;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.noah.superagent.common.enums.ChatTaskStatusEnum;
import cn.hutool.core.util.IdUtil;
import com.noah.superagent.common.constants.BusinessConstants;
import com.noah.superagent.common.enums.EnabledEnum;
import com.noah.superagent.dao.entity.ChatTaskEntity;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.entity.ScheduledChatTaskExecutionLogEntity;
import com.noah.superagent.dao.mapper.ChatTaskMapper;
import com.noah.superagent.dao.mapper.ScheduledChatTaskExecutionLogMapper;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

/**
 * 定时聊天任务执行器
 * <p>
 * 使用 db-scheduler 的 OneTimeTask 实现精确的定时聊天任务执行
 * 每个定时聊天任务会创建一个独立的执行实例，精确在指定时间执行
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Component
public class ScheduledChatExecutionTask {

    public static final String TASK_NAME = "scheduled-chat-execution";

    private final ScheduledChatTaskMapper scheduledChatTaskMapper;
    private final ScheduledChatTaskExecutionLogMapper executionLogMapper;
    private final ChatTaskMapper chatTaskMapper;
    private final ScheduledChatTaskSchedulingTask schedulingTask;

    @Getter
    private final OneTimeTask<ChatTaskData> task;

    public ScheduledChatExecutionTask(ScheduledChatTaskMapper scheduledChatTaskMapper,
                                    ScheduledChatTaskExecutionLogMapper executionLogMapper,
                                    ChatTaskMapper chatTaskMapper,
                                    @Lazy ScheduledChatTaskSchedulingTask schedulingTask) {
        this.scheduledChatTaskMapper = scheduledChatTaskMapper;
        this.executionLogMapper = executionLogMapper;
        this.chatTaskMapper = chatTaskMapper;
        this.schedulingTask = schedulingTask;

        // 创建一次性任务，用于执行定时聊天
        this.task = Tasks.oneTime(TASK_NAME, ChatTaskData.class)
                .execute(this::executeChatTask);
    }

    /**
     * 执行单个聊天任务
     */
    private void executeChatTask(TaskInstance<ChatTaskData> taskInstance, ExecutionContext executionContext) {
        ChatTaskData data = taskInstance.getData();
        Long taskId = data.getTaskId();
        
        log.info("【定时聊天】开始执行任务 - taskId: {}, taskName: {}", 
                taskId, data.getTaskName());

        // 创建执行日志
        ScheduledChatTaskExecutionLogEntity executionLog = createExecutionLog(data);

        try {
            // 查询任务详情
            ScheduledChatTaskEntity scheduledTask = scheduledChatTaskMapper.selectOneById(taskId);
            if (scheduledTask == null || scheduledTask.getStatus() != 1) {
                log.warn("【定时聊天】任务不存在或已禁用 - taskId: {}", taskId);
                executionLog.setErrorMessage("任务不存在或已禁用");
                return;
            }

            // 查找或创建关联的对话任务
            ChatTaskEntity chatTask = findOrCreateChatTask(scheduledTask, data);
            executionLog.setChatTaskId(chatTask.getId());

            // 执行定时任务
            String response = executeScheduledTask(scheduledTask);

            // 更新对话任务
            updateChatTaskWithResponse(chatTask, response);

            // 处理任务调度：一次性任务禁用，重复任务安排下次执行
            handleTaskScheduling(scheduledTask, data);

            // 记录成功状态
            executionLog.setExecutionStatus(1);
            executionLog.setExecutionResult(response);

            log.info("【定时聊天】任务执行成功 - taskId: {}, taskName: {}", taskId, data.getTaskName());

        } catch (Exception e) {
            log.error("【定时聊天】任务执行失败 - taskId: {}, error: {}", taskId, e.getMessage(), e);
            executionLog.setErrorMessage(e.getMessage());
            throw new RuntimeException("定时聊天任务执行失败", e);
        } finally {
            // 完成执行日志
            completeExecutionLog(executionLog);
        }
    }

    /**
     * 创建执行日志
     */
    private ScheduledChatTaskExecutionLogEntity createExecutionLog(ChatTaskData data) {
        ScheduledChatTaskExecutionLogEntity log = new ScheduledChatTaskExecutionLogEntity();
        log.setTaskId(data.getTaskId());
        log.setTaskName(data.getTaskName());
        log.setStartTime(new Date());
        log.setExecutionStatus(0); // 默认失败状态
        return log;
    }

    /**
     * 查找或创建关联的对话任务
     */
    private ChatTaskEntity findOrCreateChatTask(ScheduledChatTaskEntity scheduledTask, ChatTaskData data) {
        return Optional.ofNullable(scheduledTask.getChatTaskId())
                .map(chatTaskMapper::selectOneById)
                .orElseGet(() -> createNewChatTask(scheduledTask, data));
    }

    /**
     * 创建新的对话任务
     */
    private ChatTaskEntity createNewChatTask(ScheduledChatTaskEntity scheduledTask, ChatTaskData data) {
        ChatTaskEntity chatTask = new ChatTaskEntity();
        chatTask.setWorkspaceId(data.getWorkspaceId());
        chatTask.setContextId("ctx_" + IdUtil.getSnowflakeNextIdStr());
        chatTask.setTitle(data.getTaskName());
        chatTask.setContent("");
        chatTask.setStatus(ChatTaskStatusEnum.IN_PROGRESS);
        chatTask.setCreateBy(data.getUserId());
        chatTask.setUpdateBy(data.getUserId());
        chatTaskMapper.insert(chatTask);

        // 更新定时任务的关联ID
        scheduledTask.setChatTaskId(chatTask.getId());
        scheduledChatTaskMapper.update(scheduledTask);

        log.info("【定时聊天】创建新对话任务 - taskId: {}, chatTaskId: {}", scheduledTask.getId(), chatTask.getId());
        return chatTask;
    }

    /**
     * 执行定时任务（不再调用AI，仅记录任务执行）
     */
    private String executeScheduledTask(ScheduledChatTaskEntity scheduledTask) {
        try {
            // 记录任务执行信息
            String executionMessage = String.format("定时任务已执行 - 任务ID: %d, 提示词: %s, 执行时间: %s", 
                    scheduledTask.getId(), 
                    scheduledTask.getPrompt(),
                    java.time.LocalDateTime.now().toString());
            
            log.info("【定时聊天】任务执行完成 - taskId: {}, prompt: {}", 
                    scheduledTask.getId(), scheduledTask.getPrompt());
            
            return executionMessage;
        } catch (Exception e) {
            log.error("【定时聊天】任务执行失败 - taskId: {}", scheduledTask.getId(), e);
            throw new RuntimeException("定时任务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新对话任务内容
     */
    private void updateChatTaskWithResponse(ChatTaskEntity chatTask, String response) {
        String newContent = StringUtils.hasText(chatTask.getContent()) 
                ? chatTask.getContent() + "\n---\n" + response 
                : response;
        
        chatTask.setContent(newContent);
        chatTask.setStatus(ChatTaskStatusEnum.COMPLETED);
        chatTask.setUpdateTime(LocalDateTime.now());
        chatTaskMapper.update(chatTask);
    }

    /**
     * 处理任务调度逻辑
     */
    private void handleTaskScheduling(ScheduledChatTaskEntity scheduledTask, ChatTaskData data) {
        if (data.getTaskType().equals(BusinessConstants.TaskType.NORMAL)) {
            // 一次性任务：执行后禁用
            scheduledTask.setStatus(EnabledEnum.DISABLED.getCode());
            scheduledTask.setLastExecutionTime(new Date());
            scheduledTask.setNextExecutionTime(null);
            scheduledChatTaskMapper.update(scheduledTask);
            
            log.info("【定时聊天】一次性任务执行完成，已禁用 - taskId: {}", scheduledTask.getId());
        } else {
            // 重复任务：发布事件重新安排下次执行
            // 这里会通过事件机制由 ScheduledChatTaskSchedulingTask 处理
            publishTaskCompletedEvent(scheduledTask, data);
        }
    }

    /**
     * 发布任务完成事件（用于重复任务的下次调度）
     */
    private void publishTaskCompletedEvent(ScheduledChatTaskEntity scheduledTask, ChatTaskData data) {
        // 更新最后执行时间
        scheduledTask.setLastExecutionTime(new Date());
        scheduledChatTaskMapper.update(scheduledTask);

        // 安排重复任务的下次执行
        if (StringUtils.hasText(data.getCronExpression())) {
            schedulingTask.scheduleNextExecution(scheduledTask.getId(), data.getCronExpression());
        } else {
            log.warn("【定时聊天】无法安排重复任务下次执行，缺少CRON表达式 - taskId: {}", scheduledTask.getId());
        }
    }

    /**
     * 完成执行日志记录
     */
    private void completeExecutionLog(ScheduledChatTaskExecutionLogEntity executionLog) {
        executionLog.setEndTime(new Date());
        executionLog.setDuration(System.currentTimeMillis() - executionLog.getStartTime().getTime());
        executionLogMapper.insertSelective(executionLog);
    }



    /**
     * 聊天任务数据
     */
    @Data
    public static class ChatTaskData {
        /**
         * 任务ID
         */
        private Long taskId;

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 工作空间ID
         */
        private Long workspaceId;

        /**
         * 任务名称
         */
        private String taskName;

        /**
         * 任务类型：0-一次性任务 1-可重复任务
         */
        private Integer taskType;

        /**
         * CRON表达式（用于重复任务重新调度）
         */
        private String cronExpression;
    }
}
