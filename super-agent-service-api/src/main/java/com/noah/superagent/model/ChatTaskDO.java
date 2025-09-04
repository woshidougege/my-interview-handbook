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
     * 关联工作空间ID
     */
    private Long workspaceId;

    /**
     * 会话ID，用于与下游平台通信
     */
    private String sessionId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 是否收藏：1是 0否
     */
    private Integer isFavorite;

    /**
     * 任务状态：1进行中 2已完成 3已归档
     */
    private Integer status;

}
