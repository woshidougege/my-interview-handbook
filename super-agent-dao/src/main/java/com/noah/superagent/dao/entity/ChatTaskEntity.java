package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话任务表
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_workspace_chat_task")
public class ChatTaskEntity extends BaseEntity {

    /**
     * 关联工作空间ID
     */
    private Long workspaceId;

    /**
     * 会话ID，用于与下游平台通信
     */
    private String contextId;

    /**
     * 对话任务标题
     */
    private String title;

    /**
     * 对话任务内容
     */
    private String content;

    /**
     * 是否收藏：1是 0否
     */
    private Integer isFavorite;

    /**
     * 状态: 1进行中 2已完成 3已归档
     */
    private Integer status;
}