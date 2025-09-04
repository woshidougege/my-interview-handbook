package com.noah.superagent.common.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础请求DTO
 * 包含系统字段用于解决MapStruct映射问题
 * 这些字段对前端隐藏，仅用于内部转换
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
public abstract class BaseRequest {

    /**
     * 主键ID - 前端可传递（如更新操作需要指定ID）
     */
    @Schema(description = "资源ID", example = "1234567890123456789")
    private Long id;

    /**
     * 创建时间 - 对前端隐藏，系统自动管理
     */
    @Schema(hidden = true)
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间 - 对前端隐藏，系统自动管理
     */
    @Schema(hidden = true)
    @JsonIgnore
    private LocalDateTime updateTime;

    /**
     * 创建人ID - 对前端隐藏，系统自动管理
     */
    @Schema(hidden = true)
    @JsonIgnore
    private Long createBy;

    /**
     * 更新人ID - 对前端隐藏，系统自动管理
     */
    @Schema(hidden = true)
    @JsonIgnore
    private Long updateBy;

    /**
     * 删除标记 - 对前端隐藏，系统自动管理
     */
    @Schema(hidden = true)
    @JsonIgnore
    private Integer deleted;

}
