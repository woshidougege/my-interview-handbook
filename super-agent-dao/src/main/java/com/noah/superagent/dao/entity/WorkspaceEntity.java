package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.DefaultEnum;
import com.noah.superagent.common.enums.WorkspaceStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户工作空间表
 *
 * @author 任相鹏
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
     * 是否默认工作空间：DEFAULT-默认 NOT_DEFAULT-非默认
     */
    private DefaultEnum isDefault;

    /**
     * 状态: NORMAL-正常 DISABLED-禁用
     */
    private WorkspaceStatusEnum status;
}