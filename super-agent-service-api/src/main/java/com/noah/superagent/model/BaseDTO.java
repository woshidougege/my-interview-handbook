package com.noah.superagent.model;

import com.noah.superagent.common.enums.DeletedEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据传输对象
 * 包含通用的审计字段
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
public abstract class BaseDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;

    /**
     * 删除标记：NOT_DELETED-未删除，DELETED-已删除
     */
    private DeletedEnum deleted;

    // 通用业务方法

    /**
     * 判断是否为新对象（未持久化）
     */
    public boolean isNew() {
        return this.id == null;
    }

}
