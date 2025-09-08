package com.noah.superagent.model;

import com.noah.superagent.common.enums.DefaultEnum;
import com.noah.superagent.common.enums.WorkspaceStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作空间领域对象
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkspaceDTO extends BaseDTO {

    /**
     * 关联用户ID（所有者ID）
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
     * 状态：1-正常，2-禁用
     */
    private WorkspaceStatusEnum status;

}
