package com.noah.superagent.service.impl;

import cn.hutool.core.util.IdUtil;
import com.noah.superagent.common.enums.CreditDeductionTaskStatusEnum;
import com.noah.superagent.dao.entity.CreditDeductionTaskEntity;
import com.noah.superagent.dao.mapper.CreditDeductionTaskMapper;
import com.noah.superagent.service.CreditConsumeService;
import com.noah.superagent.service.CreditDeductionTaskService;
import com.noah.superagent.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分扣减任务服务实现
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditDeductionTaskServiceImpl implements CreditDeductionTaskService {

    private final CreditDeductionTaskMapper creditDeductionTaskMapper;
    private final CreditConsumeService creditConsumeService;
    private final UserCreditService userCreditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String scheduleImmediateCreditDeduction(Long userId, 
                                                 BigDecimal amount, 
                                                 String description, 
                                                 Long relatedOrderId, 
                                                 Long resourceUsageRecordId) {
        
        String taskId = generateTaskId(userId, resourceUsageRecordId);
        
        log.info("开始立即执行积分扣减 - taskId: {}, userId: {}, amount: {}", taskId, userId, amount);
        
        try {
            // 1. 检查用户是否可以消费积分（包含透支检查）
            boolean canConsume = userCreditService.canConsumeCredits(userId, amount);
            if (!canConsume) {
                throw new RuntimeException("积分余额不足（含透支额度）");
            }
            
            // 2. 执行积分扣减
            creditConsumeService.consumeCredits(
                    userId, 
                    amount, 
                    description, 
                    relatedOrderId);
            
            // 3. 创建成功记录
            createSuccessTaskRecord(taskId, userId, amount, description, relatedOrderId, resourceUsageRecordId);
            
            log.info("积分扣减立即执行成功 - taskId: {}, userId: {}, amount: {}", taskId, userId, amount);
            
        } catch (Exception e) {
            log.error("积分扣减立即执行失败 - taskId: {}, userId: {}, amount: {}, 错误: {}", 
                    taskId, userId, amount, e.getMessage(), e);
            
            // 创建失败记录，等待清理任务重试
            createFailedTaskRecord(taskId, userId, amount, description, relatedOrderId, resourceUsageRecordId, e.getMessage());
            
            // 不抛出异常，让用户请求正常返回，失败的任务可以通过清理任务重试
        }
        
        return taskId;
    }

    /**
     * 创建成功的任务记录
     */
    private void createSuccessTaskRecord(String taskId, Long userId, BigDecimal amount, 
                                       String description, Long relatedOrderId, Long resourceUsageRecordId) {
        CreditDeductionTaskEntity task = new CreditDeductionTaskEntity();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setAmount(amount);
        task.setDescription(description);
        task.setRelatedOrderId(relatedOrderId);
        task.setResourceUsageRecordId(resourceUsageRecordId);
        task.setStatus(CreditDeductionTaskStatusEnum.SUCCESS);
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setScheduledTime(LocalDateTime.now());
        task.setExecutedTime(LocalDateTime.now());
        task.setCompletedTime(LocalDateTime.now());
        task.setCreateBy(userId);
        
        creditDeductionTaskMapper.insert(task);
    }

    /**
     * 创建失败的任务记录
     */
    private void createFailedTaskRecord(String taskId, Long userId, BigDecimal amount, 
                                      String description, Long relatedOrderId, Long resourceUsageRecordId, String errorMessage) {
        CreditDeductionTaskEntity task = new CreditDeductionTaskEntity();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setAmount(amount);
        task.setDescription(description);
        task.setRelatedOrderId(relatedOrderId);
        task.setResourceUsageRecordId(resourceUsageRecordId);
        task.setStatus(CreditDeductionTaskStatusEnum.PENDING); // 设置为待处理，等待重试
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setScheduledTime(LocalDateTime.now());
        task.setErrorMessage(errorMessage);
        task.setLastErrorTime(LocalDateTime.now());
        task.setCreateBy(userId);
        
        creditDeductionTaskMapper.insert(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskToProcessing(String taskId) {
        creditDeductionTaskMapper.updateTaskStatus(
                taskId, 
                CreditDeductionTaskStatusEnum.PENDING,
                CreditDeductionTaskStatusEnum.PROCESSING,
                LocalDateTime.now());
        
        log.debug("任务状态更新为处理中 - taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskToSuccess(String taskId) {
        creditDeductionTaskMapper.updateTaskCompleted(
                taskId,
                CreditDeductionTaskStatusEnum.SUCCESS,
                LocalDateTime.now(),
                null);
        
        log.debug("任务状态更新为成功 - taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskToFailed(String taskId, String errorMessage) {
        creditDeductionTaskMapper.incrementRetryCount(
                taskId,
                errorMessage,
                LocalDateTime.now());
        
        log.debug("任务重试计数增加 - taskId: {}, 错误: {}", taskId, errorMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processPendingTasks(int maxTasks) {
        log.debug("开始处理待处理的积分扣减任务，最大处理数量: {}", maxTasks);
        
        // 1. 查询待处理的任务
        List<CreditDeductionTaskEntity> pendingTasks = creditDeductionTaskMapper.selectPendingTasks(maxTasks);
        if (pendingTasks.isEmpty()) {
            log.debug("没有待处理的积分扣减任务");
            return 0;
        }
        
        log.info("找到 {} 个待处理的积分扣减任务", pendingTasks.size());
        
        int processedCount = 0;
        for (CreditDeductionTaskEntity task : pendingTasks) {
            try {
                processTask(task);
                processedCount++;
            } catch (Exception e) {
                log.error("处理积分扣减任务失败 - taskId: {}", task.getTaskId(), e);
                handleTaskError(task, e.getMessage());
            }
        }
        
        log.info("积分扣减任务处理完成 - 处理数量: {}/{}", processedCount, pendingTasks.size());
        return processedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetTimeoutTasks(int timeoutMinutes, int maxTasks) {
        log.debug("开始重置超时任务，超时分钟数: {}，最大重置数量: {}", timeoutMinutes, maxTasks);
        
        // 1. 查询超时的处理中任务
        List<CreditDeductionTaskEntity> timeoutTasks = 
            creditDeductionTaskMapper.selectTimeoutProcessingTasks(timeoutMinutes, maxTasks);
        
        if (timeoutTasks.isEmpty()) {
            log.debug("没有超时的积分扣减任务");
            return 0;
        }
        
        List<String> taskIds = timeoutTasks.stream()
                .map(CreditDeductionTaskEntity::getTaskId)
                .collect(Collectors.toList());
        
        // 2. 重置任务状态为待处理
        int resetCount = creditDeductionTaskMapper.resetTimeoutTasksToPending(taskIds);
        
        log.warn("重置超时积分扣减任务 - 重置数量: {}, 任务IDs: {}", resetCount, taskIds);
        return resetCount;
    }

    /**
     * 处理单个任务
     */
    private void processTask(CreditDeductionTaskEntity task) {
        String taskId = task.getTaskId();
        log.info("开始处理积分扣减任务 - taskId: {}, userId: {}, amount: {}", 
                 taskId, task.getUserId(), task.getAmount());
        
        // 1. 更新任务状态为处理中
        int updateResult = creditDeductionTaskMapper.updateTaskStatus(
                taskId, 
                CreditDeductionTaskStatusEnum.PENDING, 
                CreditDeductionTaskStatusEnum.PROCESSING,
                LocalDateTime.now());
        
        if (updateResult <= 0) {
            log.warn("任务可能已被其他进程处理 - taskId: {}", taskId);
            return;
        }
        
        try {
            // 2. 检查用户是否可以消费积分（包含透支检查）
            boolean canConsume = userCreditService.canConsumeCredits(task.getUserId(), task.getAmount());
            if (!canConsume) {
                throw new RuntimeException("积分余额不足（含透支额度）");
            }
            
            // 3. 执行积分扣减
            creditConsumeService.consumeCredits(
                    task.getUserId(), 
                    task.getAmount(), 
                    task.getDescription(), 
                    task.getRelatedOrderId());
            
            // 4. 更新任务状态为成功
            creditDeductionTaskMapper.updateTaskCompleted(
                    taskId, 
                    CreditDeductionTaskStatusEnum.SUCCESS, 
                    LocalDateTime.now(), 
                    null);
            
            log.info("积分扣减任务处理成功 - taskId: {}", taskId);
            
        } catch (Exception e) {
            log.error("积分扣减任务执行失败 - taskId: {}", taskId, e);
            
            // 更新任务状态为失败
            creditDeductionTaskMapper.updateTaskCompleted(
                    taskId, 
                    CreditDeductionTaskStatusEnum.FAILED, 
                    LocalDateTime.now(), 
                    e.getMessage());
            
            throw e;
        }
    }

    /**
     * 处理任务错误，增加重试次数
     */
    private void handleTaskError(CreditDeductionTaskEntity task, String errorMessage) {
        try {
            creditDeductionTaskMapper.incrementRetryCount(
                    task.getTaskId(), 
                    errorMessage, 
                    LocalDateTime.now());
            
            log.warn("积分扣减任务重试计数增加 - taskId: {}, 当前重试次数: {}/{}", 
                     task.getTaskId(), task.getRetryCount() + 1, task.getMaxRetryCount());
                     
        } catch (Exception e) {
            log.error("更新任务重试次数失败 - taskId: {}", task.getTaskId(), e);
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(Long userId, Long resourceUsageRecordId) {
        String timestamp = IdUtil.getSnowflakeNextIdStr();
        String randomStr = IdUtil.fastSimpleUUID().substring(0, 8);
        return String.format("CREDIT_DEDUCT_%s_%s_%s_%s", 
                            userId, resourceUsageRecordId != null ? resourceUsageRecordId : "0", timestamp, randomStr);
    }
}
