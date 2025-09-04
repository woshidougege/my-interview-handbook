package com.noah.superagent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话任务领域对象
 * 
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatTaskDO extends BaseDO {

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 任务状态：0-待处理，1-处理中，2-已完成，3-已取消
     */
    private Integer status;

}
