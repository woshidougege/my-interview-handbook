package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 分页请求DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@Schema(description = "分页请求")
public class PageRequest {

    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "搜索关键词", example = "张三")
    private String keyword;
}
