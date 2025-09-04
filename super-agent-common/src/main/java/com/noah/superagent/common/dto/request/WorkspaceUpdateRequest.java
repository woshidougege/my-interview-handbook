package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作空间更新请求DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作空间更新请求")
public class WorkspaceUpdateRequest extends BaseRequest {

    @Schema(description = "工作空间名称", example = "开发工作空间")
    private String name;

    @Schema(description = "工作空间描述", example = "用于项目开发的工作空间")
    private String description;

    @Schema(description = "是否默认工作空间：1是 0否", example = "1")
    private Integer isDefault;

    @Schema(description = "状态: 1正常 2禁用", example = "1")
    private Integer status;
}