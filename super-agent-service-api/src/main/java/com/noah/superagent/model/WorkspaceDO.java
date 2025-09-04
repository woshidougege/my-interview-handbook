package com.noah.superagent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作空间领域对象
 * 
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkspaceDO extends BaseDO {

    /**
     * 工作空间名称
     */
    private String name;

    /**
     * 工作空间描述
     */
    private String description;

    /**
     * 所有者ID
     */
    private Long ownerId;

    /**
     * 状态：0-正常，1-禁用
     */
    private Integer status;

}
