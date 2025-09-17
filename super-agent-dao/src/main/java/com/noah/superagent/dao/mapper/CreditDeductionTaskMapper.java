package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.CreditDeductionTaskEntity;
import com.noah.superagent.common.enums.CreditDeductionTaskStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

import static com.noah.superagent.dao.entity.table.CreditDeductionTaskEntityTableDef.CREDIT_DEDUCTION_TASK_ENTITY;

/**
 * 积分扣减任务 Mapper
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface CreditDeductionTaskMapper extends BaseMapper<CreditDeductionTaskEntity> {

    /**
     * 查询待处理的任务
     * @param limit 限制数量
     * @return 待处理的任务列表
     */
    default List<CreditDeductionTaskEntity> selectPendingTasks(int limit) {
        return selectListByQuery(QueryWrapper.create()
                .select()
                .where(CREDIT_DEDUCTION_TASK_ENTITY.DELETED.eq(0))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.STATUS.eq(CreditDeductionTaskStatusEnum.PENDING))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.SCHEDULED_TIME.le(LocalDateTime.now()))
                .orderBy(CREDIT_DEDUCTION_TASK_ENTITY.SCHEDULED_TIME.asc(), CREDIT_DEDUCTION_TASK_ENTITY.CREATE_TIME.asc())
                .limit(limit)
        );
    }

    /**
     * 查询超时未完成的处理中任务（可能是死任务，需要重置）
     * @param timeoutMinutes 超时分钟数
     * @param limit 限制数量
     * @return 超时的处理中任务列表
     */
    default List<CreditDeductionTaskEntity> selectTimeoutProcessingTasks(int timeoutMinutes, int limit) {
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return selectListByQuery(QueryWrapper.create()
                .select()
                .where(CREDIT_DEDUCTION_TASK_ENTITY.DELETED.eq(0))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.STATUS.eq(CreditDeductionTaskStatusEnum.PROCESSING))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.EXECUTED_TIME.lt(timeoutTime))
                .orderBy(CREDIT_DEDUCTION_TASK_ENTITY.EXECUTED_TIME.asc())
                .limit(limit)
        );
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param executedTime 执行时间
     * @return 更新数量
     */
    default int updateTaskStatus(String taskId, CreditDeductionTaskStatusEnum oldStatus,
                               CreditDeductionTaskStatusEnum newStatus, LocalDateTime executedTime) {
        // 先查询任务
        CreditDeductionTaskEntity task = selectOneByQuery(QueryWrapper.create()
                .select()
                .where(CREDIT_DEDUCTION_TASK_ENTITY.TASK_ID.eq(taskId))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.STATUS.eq(oldStatus))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.DELETED.eq(0))
        );
        
        if (task == null) {
            return 0;
        }
        
        // 更新状态
        task.setStatus(newStatus);
        task.setExecutedTime(executedTime);
        task.setUpdateTime(LocalDateTime.now());
        
        return update(task);
    }

    /**
     * 更新任务为完成状态
     * @param taskId 任务ID
     * @param status 完成状态（成功/失败）
     * @param completedTime 完成时间
     * @param errorMessage 错误信息（可为空）
     * @return 更新数量
     */
    default int updateTaskCompleted(String taskId, CreditDeductionTaskStatusEnum status,
                                  LocalDateTime completedTime, String errorMessage) {
        // 先查询任务
        CreditDeductionTaskEntity task = selectOneByQuery(QueryWrapper.create()
                .select()
                .where(CREDIT_DEDUCTION_TASK_ENTITY.TASK_ID.eq(taskId))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.DELETED.eq(0))
        );
        
        if (task == null) {
            return 0;
        }
        
        // 更新状态
        task.setStatus(status);
        task.setCompletedTime(completedTime);
        task.setUpdateTime(LocalDateTime.now());
        
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
            task.setLastErrorTime(completedTime);
        }
        
        return update(task);
    }

    /**
     * 增加任务重试次数并记录错误
     * @param taskId 任务ID  
     * @param errorMessage 错误信息
     * @param lastErrorTime 错误时间
     * @return 更新数量
     */
    default int incrementRetryCount(String taskId, String errorMessage, LocalDateTime lastErrorTime) {
        // 先查询当前任务
        CreditDeductionTaskEntity task = selectOneByQuery(QueryWrapper.create()
                .select()
                .where(CREDIT_DEDUCTION_TASK_ENTITY.TASK_ID.eq(taskId))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.DELETED.eq(0))
        );
        
        if (task == null) {
            return 0;
        }
        
        // 更新重试计数
        int newRetryCount = task.getRetryCount() + 1;
        CreditDeductionTaskStatusEnum newStatus = newRetryCount >= task.getMaxRetryCount() 
            ? CreditDeductionTaskStatusEnum.FAILED 
            : CreditDeductionTaskStatusEnum.PENDING;
        
        task.setRetryCount(newRetryCount);
        task.setErrorMessage(errorMessage);
        task.setLastErrorTime(lastErrorTime);
        task.setStatus(newStatus);
        task.setUpdateTime(LocalDateTime.now());
        
        return update(task);
    }

    /**
     * 重置超时任务状态为待处理
     * @param taskIds 任务ID列表
     * @return 更新数量
     */
    default int resetTimeoutTasksToPending(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return 0;
        }
        
        // 查询所有需要重置的任务
        List<CreditDeductionTaskEntity> tasks = selectListByQuery(QueryWrapper.create()
                .select()
                .where(CREDIT_DEDUCTION_TASK_ENTITY.TASK_ID.in(taskIds))
                .and(CREDIT_DEDUCTION_TASK_ENTITY.DELETED.eq(0))
        );
        
        int updateCount = 0;
        for (CreditDeductionTaskEntity task : tasks) {
            task.setStatus(CreditDeductionTaskStatusEnum.PENDING);
            task.setExecutedTime(null);
            task.setUpdateTime(LocalDateTime.now());
            updateCount += update(task);
        }
        
        return updateCount;
    }
}
