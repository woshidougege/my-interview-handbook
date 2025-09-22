package com.noah.superagent.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.dto.ScheduleConfig;
import com.noah.superagent.common.dto.request.ScheduledChatTaskCreateRequest;
import com.noah.superagent.common.dto.request.ScheduledChatTaskUpdateRequest;
import com.noah.superagent.common.dto.response.ScheduledChatTaskResponse;
import com.noah.superagent.common.util.CronExpressionUtil;
import com.noah.superagent.model.ScheduledChatTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时对话任务Web层转换器实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Component
public class ScheduledChatTaskWebConvertImpl extends ScheduledChatTaskWebConvert {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从创建请求转换为DTO
     *
     * @param request 创建请求
     * @return DTO
     */
    @Override
    public ScheduledChatTaskDTO fromCreateRequest(ScheduledChatTaskCreateRequest request) {
        ScheduledChatTaskDTO dto = new ScheduledChatTaskDTO();
        dto.setUserId(request.getUserId());
        dto.setWorkspaceId(request.getWorkspaceId());
        // 任务名称自动截取前10个字符，如果为空则从prompt中生成
        dto.setTaskName(generateTaskName(request.getTaskName(), request.getPrompt()));
        dto.setPrompt(request.getPrompt());
        dto.setStatus(request.getStatus());
        // 根据调度配置自动判断任务类型
        dto.setTaskType(determineTaskType(request));

        // 处理调度配置
        if (request.getScheduleConfig() != null) {
            try {
                // 生成cron表达式
                String cronExpression = CronExpressionUtil.generateCronExpression(request.getScheduleConfig());
                dto.setCronExpression(cronExpression);
                
                // 存储配置信息用于回显
                ScheduleConfig scheduleConfig = new ScheduleConfig();
                scheduleConfig.setCycleType(request.getScheduleConfig().getCycleType());
                scheduleConfig.setExecutionTime(request.getScheduleConfig().getExecutionTime());
                scheduleConfig.setExecutionDate(request.getScheduleConfig().getExecutionDate());
                scheduleConfig.setDayOfWeek(request.getScheduleConfig().getDayOfWeek());
                scheduleConfig.setDayOfMonth(request.getScheduleConfig().getDayOfMonth());
                dto.setScheduleConfig(scheduleConfig);
            } catch (Exception e) {
                log.error("生成cron表达式失败", e);
                throw new RuntimeException("生成定时任务调度表达式失败: " + e.getMessage(), e);
            }
        } else if (request.getCronExpression() != null && !request.getCronExpression().isEmpty()) {
            // 兼容旧版本直接传递cron表达式的方式
            dto.setCronExpression(request.getCronExpression());
        } else {
            throw new RuntimeException("必须提供调度配置信息");
        }

        return dto;
    }

    /**
     * 从更新请求转换为DTO
     *
     * @param request 更新请求
     * @return DTO
     */
    @Override
    public ScheduledChatTaskDTO fromUpdateRequest(ScheduledChatTaskUpdateRequest request) {
        ScheduledChatTaskDTO dto = new ScheduledChatTaskDTO();
        // 任务名称自动截取前10个字符，如果为空则从prompt中生成
        dto.setTaskName(generateTaskName(request.getTaskName(), request.getPrompt()));
        dto.setPrompt(request.getPrompt());
        dto.setStatus(request.getStatus());
        dto.setChatTaskId(request.getChatTaskId());
        // 根据调度配置自动判断任务类型
        dto.setTaskType(determineTaskType(request));

        // 处理调度配置
        if (request.getScheduleConfig() != null) {
            try {
                // 生成cron表达式
                String cronExpression = CronExpressionUtil.generateCronExpression(request.getScheduleConfig());
                if (cronExpression != null) {
                    dto.setCronExpression(cronExpression);
                }
                
                // 存储配置信息用于回显
                ScheduleConfig scheduleConfig = new ScheduleConfig();
                scheduleConfig.setCycleType(request.getScheduleConfig().getCycleType());
                scheduleConfig.setExecutionTime(request.getScheduleConfig().getExecutionTime());
                scheduleConfig.setExecutionDate(request.getScheduleConfig().getExecutionDate());
                scheduleConfig.setDayOfWeek(request.getScheduleConfig().getDayOfWeek());
                scheduleConfig.setDayOfMonth(request.getScheduleConfig().getDayOfMonth());
                dto.setScheduleConfig(scheduleConfig);
            } catch (Exception e) {
                log.error("生成cron表达式失败", e);
                throw new RuntimeException("生成定时任务调度表达式失败: " + e.getMessage(), e);
            }
        } else if (request.getCronExpression() != null && !request.getCronExpression().isEmpty()) {
            // 兼容旧版本直接传递cron表达式的方式
            dto.setCronExpression(request.getCronExpression());
        }

        return dto;
    }

    /**
     * 从DTO转换为响应
     *
     * @param dto DTO
     * @return 响应
     */
    @Override
    public ScheduledChatTaskResponse toResponse(ScheduledChatTaskDTO dto) {
        ScheduledChatTaskResponse response = new ScheduledChatTaskResponse();
        response.setId(dto.getId());
        response.setUserId(dto.getUserId());
        response.setWorkspaceId(dto.getWorkspaceId());
        response.setChatTaskId(dto.getChatTaskId());
        // 任务名称自动截取前10个字符
        response.setTaskName(trimTaskName(dto.getTaskName()));
        response.setPrompt(dto.getPrompt());
        response.setStatus(dto.getStatus());
        response.setLastExecutionTime(dto.getLastExecutionTime());
        response.setNextExecutionTime(dto.getNextExecutionTime());
        // 根据调度配置自动判断任务类型
        response.setTaskType(determineTaskType(dto));
        response.setCronExpression(dto.getCronExpression());

        // 处理调度配置回显
        if (dto.getScheduleConfig() != null) {
            response.setScheduleConfig(dto.getScheduleConfig());
        } else if (dto.getCronExpression() != null && !dto.getCronExpression().isEmpty()) {
            // 从cron表达式解析出调度配置用于回显
            ScheduleConfig config = CronExpressionUtil.parseCronExpression(dto.getCronExpression());
            response.setScheduleConfig(config);
        }

        return response;
    }
    
    /**
     * 从DTO列表转换为响应列表
     *
     * @param dtos DTO列表
     * @return 响应列表
     */
    @Override
    public List<ScheduledChatTaskResponse> toResponseList(List<ScheduledChatTaskDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        
        List<ScheduledChatTaskResponse> responses = new ArrayList<>();
        for (ScheduledChatTaskDTO dto : dtos) {
            responses.add(toResponse(dto));
        }
        
        return responses;
    }
    
    /**
     * 截取任务名称前10个字符
     * 
     * @param taskName 原始任务名称
     * @return 截取后的任务名称
     */
    private String trimTaskName(String taskName) {
        if (StringUtils.isBlank(taskName)) {
            return "未命名任务";
        }
        
        if (taskName.length() > 10) {
            return taskName.substring(0, 10);
        }
        
        return taskName;
    }
    
    /**
     * 生成任务名称：如果任务名称不为空则使用任务名称，否则从prompt中截取
     * 
     * @param taskName 任务名称
     * @param prompt 提示词
     * @return 生成的任务名称
     */
    private String generateTaskName(String taskName, String prompt) {
        // 如果任务名称不为空，直接使用并截取前10个字符
        if (StringUtils.isNotBlank(taskName)) {
            return trimTaskName(taskName);
        }
        
        // 如果任务名称为空，从prompt中截取前10个字符作为任务名称
        if (StringUtils.isNotBlank(prompt)) {
            return trimTaskName(prompt);
        }
        
        // 如果任务名称和prompt都为空，使用默认名称
        return "未命名任务";
    }
    
    /**
     * 根据调度配置确定任务类型
     * 
     * @param request 创建或更新请求
     * @return 任务类型 (0-一次性任务, 1-可重复任务)
     */
    private Integer determineTaskType(ScheduledChatTaskCreateRequest request) {
        if (request.getScheduleConfig() != null) {
            String cycleType = request.getScheduleConfig().getCycleType();
            if ("ONE_TIME".equalsIgnoreCase(cycleType)) {
                return 0; // 一次性任务
            }
        }
        return 1; // 可重复任务（默认）
    }
    
    /**
     * 根据调度配置确定任务类型
     * 
     * @param request 更新请求
     * @return 任务类型 (0-一次性任务, 1-可重复任务)
     */
    private Integer determineTaskType(ScheduledChatTaskUpdateRequest request) {
        if (request.getScheduleConfig() != null) {
            String cycleType = request.getScheduleConfig().getCycleType();
            if ("ONE_TIME".equalsIgnoreCase(cycleType)) {
                return 0; // 一次性任务
            }
        }
        return 1; // 可重复任务（默认）
    }
    
    /**
     * 根据调度配置确定任务类型
     * 
     * @param dto DTO
     * @return 任务类型 (0-一次性任务, 1-可重复任务)
     */
    private Integer determineTaskType(ScheduledChatTaskDTO dto) {
        if (dto.getScheduleConfig() != null) {
            String cycleType = dto.getScheduleConfig().getCycleType();
            if ("ONE_TIME".equalsIgnoreCase(cycleType)) {
                return 0; // 一次性任务
            }
        }
        return 1; // 可重复任务（默认）
    }
}