package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.common.enums.EnabledEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 积分类型配置表
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_credit_type_config")
public class CreditTypeConfigEntity extends BaseEntity {

    /**
     * 积分类型代码
     */
    private CreditTypeEnum typeCode;

    /**
     * 积分类型名称
     */
    private String typeName;

    /**
     * 有效期天数，0表示永久
     */
    private Integer validityDays;

    /**
     * 消费优先级，数字越小优先级越高
     */
    private Integer consumePriority;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    private EnabledEnum enabled;

    /**
     * 是否为永久积分
     */
    public boolean isPermanent() {
        return validityDays != null && validityDays == 0;
    }

    /**
     * 是否为临时积分（有有效期）
     */
    public boolean isTemporary() {
        return validityDays != null && validityDays > 0;
    }
}
