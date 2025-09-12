package com.noah.superagent.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.noah.superagent.dao.entity.ScheduledChatTaskEntity;
import com.noah.superagent.dao.mapper.ScheduledChatTaskMapper;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.service.ScheduledChatTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 定时对话任务服务实现
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledChatTaskServiceImpl implements ScheduledChatTaskService {

    private final ScheduledChatTaskMapper scheduledChatTaskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledChatTaskEntity createScheduledChatTask(ScheduledChatTaskEntity task) {
        log.info("开始创建定时对话任务，任务名称: {}", task.getTaskName());

        // 保存定时对话任务
        int result = scheduledChatTaskMapper.insertSelective(task);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务创建失败");
        }

        log.info("定时对话任务创建成功，ID: {}", task.getId());
        return task;
    }

    @Override
    public ScheduledChatTaskEntity getScheduledChatTaskById(Long id) {
        log.info("查询定时对话任务信息，ID: {}", id);

        ScheduledChatTaskEntity scheduledChatTaskEntity = scheduledChatTaskMapper.selectOneById(id);
        if (scheduledChatTaskEntity == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        return scheduledChatTaskEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduledChatTaskEntity updateScheduledChatTask(Long id, ScheduledChatTaskEntity task) {
        log.info("更新定时对话任务信息，ID: {}", id);

        // 查询定时对话任务是否存在
        ScheduledChatTaskEntity existingTask = scheduledChatTaskMapper.selectOneById(id);
        if (existingTask == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        // 更新字段
        if (StringUtils.hasText(task.getTaskName())) {
            existingTask.setTaskName(task.getTaskName());
        }

        if (StringUtils.hasText(task.getCronExpression())) {
            existingTask.setCronExpression(task.getCronExpression());
        }

        if (StringUtils.hasText(task.getPrompt())) {
            existingTask.setPrompt(task.getPrompt());
        }

        if (task.getStatus() != null) {
            existingTask.setStatus(task.getStatus());
        }

        // 执行更新
        int result = scheduledChatTaskMapper.update(existingTask);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务更新失败");
        }

        log.info("定时对话任务更新成功，ID: {}", id);
        return existingTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheduledChatTask(Long id) {
        log.info("删除定时对话任务，ID: {}", id);

        // 检查定时对话任务是否存在
        ScheduledChatTaskEntity scheduledChatTaskEntity = scheduledChatTaskMapper.selectOneById(id);
        if (scheduledChatTaskEntity == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        // 执行删除
        int result = scheduledChatTaskMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务删除失败");
        }

        log.info("定时对话任务删除成功，ID: {}", id);
    }

    @Override
    public List<ScheduledChatTaskEntity> getScheduledChatTasksByUserId(Long userId) {
        log.info("根据用户ID查询定时对话任务列表，用户ID: {}", userId);

        return scheduledChatTaskMapper.selectByUserId(userId);
    }

    @Override
    public List<ScheduledChatTaskEntity> getScheduledChatTasksByWorkspaceId(Long workspaceId) {
        log.info("根据工作空间ID查询定时对话任务列表，工作空间ID: {}", workspaceId);

        return scheduledChatTaskMapper.selectByWorkspaceId(workspaceId);
    }

    @Override
    public PageResponse<ScheduledChatTaskEntity> getScheduledChatTasksPage(Long workspaceId, Integer pageNum, Integer pageSize, String keyword) {
        log.info("分页查询定时对话任务，工作空间ID: {}, 页码: {}, 每页数量: {}, 关键词: {}",
                workspaceId, pageNum, pageSize, keyword);

        // 创建分页对象
        Page<ScheduledChatTaskEntity> page = new Page<>(pageNum, pageSize);

        // 执行分页查询
        Page<ScheduledChatTaskEntity> scheduledChatTaskPage = scheduledChatTaskMapper.selectScheduledChatTaskPage(page, workspaceId, keyword);

        // 转换结果
        return new PageResponse<>(
                scheduledChatTaskPage.getRecords(),
                scheduledChatTaskPage.getTotalRow(),
                pageNum,
                pageSize
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableScheduledChatTask(Long id) {
        log.info("启用定时对话任务，ID: {}", id);

        // 查询定时对话任务是否存在
        ScheduledChatTaskEntity existingTask = scheduledChatTaskMapper.selectOneById(id);
        if (existingTask == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        // 更新状态为启用(1)
        existingTask.setStatus(1);
        int result = scheduledChatTaskMapper.update(existingTask);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务启用失败");
        }

        log.info("定时对话任务启用成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableScheduledChatTask(Long id) {
        log.info("禁用定时对话任务，ID: {}", id);

        // 查询定时对话任务是否存在
        ScheduledChatTaskEntity existingTask = scheduledChatTaskMapper.selectOneById(id);
        if (existingTask == null) {
            throw new RuntimeException("定时对话任务不存在: " + id);
        }

        // 更新状态为禁用(0)
        existingTask.setStatus(0);
        int result = scheduledChatTaskMapper.update(existingTask);
        if (result <= 0) {
            throw new RuntimeException("定时对话任务禁用失败");
        }

        log.info("定时对话任务禁用成功，ID: {}", id);
    }
}