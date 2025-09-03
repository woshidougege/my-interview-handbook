package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户工作空间表
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user_workspace")
public class WorkspaceEntity extends BaseEntity {

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 工作空间名称
     */
    private String name;

    /**
     * 工作空间描述
     */
    private String description;

    /**
     * 是否默认工作空间：1是 0否
     */
    private Integer isDefault;

    /**
     * 状态: 1正常 2禁用
     */
    private Integer status;
}