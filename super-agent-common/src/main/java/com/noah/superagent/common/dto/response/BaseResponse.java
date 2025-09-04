package com.noah.superagent.common.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础响应DTO
 * 包含通用的系统字段
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
public abstract class BaseResponse {

    /**
     * 主键ID - 前端需要用于资源识别
     */
    @Schema(description = "资源ID", example = "1234567890123456789")
    private Long id;

    /**
     * 创建时间 - 对前端隐藏，仅用于解决转换问题
     */
    @Schema(hidden = true)
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间 - 对前端隐藏，仅用于解决转换问题
     */
    @Schema(hidden = true)
    @JsonIgnore
    private LocalDateTime updateTime;

    /**
     * 创建人ID - 对前端隐藏，仅用于解决转换问题
     */
    @Schema(hidden = true)
    @JsonIgnore
    private Long createBy;

    /**
     * 更新人ID - 对前端隐藏，仅用于解决转换问题
     */
    @Schema(hidden = true)
    @JsonIgnore
    private Long updateBy;

    /**
     * 删除标记 - 对前端隐藏，仅用于解决转换问题
     */
    @Schema(hidden = true)
    @JsonIgnore
    private Integer deleted;

}
