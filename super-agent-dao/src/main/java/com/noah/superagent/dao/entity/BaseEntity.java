package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.noah.superagent.common.enums.DeletedEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 包含6个公共字段：id、创建时间、更新时间、创建人、更新人、删除标记
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
public abstract class BaseEntity {

    /**
     * 主键ID - 雪花算法生成
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /**
     * 创建时间
     */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
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
     * 删除标记：0-未删除，1-已删除
     * 使用逻辑删除，不实际删除数据
     */
    @Column(isLogicDelete = true, onInsertValue = "0")
    private Integer deleted;

    /**
     * 判断是否已删除
     *
     * @return true-已删除，false-未删除
     */
    public boolean isDeleted() {
        return DeletedEnum.isDeleted(this.deleted);
    }

    /**
     * 判断是否未删除
     *
     * @return true-未删除，false-已删除
     */
    public boolean isNotDeleted() {
        return DeletedEnum.isNotDeleted(this.deleted);
    }

    /**
     * 设置为已删除
     */
    public void markDeleted() {
        this.deleted = DeletedEnum.DELETED.getValue();
    }

    /**
     * 设置为未删除
     */
    public void markNotDeleted() {
        this.deleted = DeletedEnum.NOT_DELETED.getValue();
    }
}
