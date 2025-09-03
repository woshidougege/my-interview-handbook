package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间响应DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@Schema(description = "工作空间信息响应")
public class WorkspaceResponse {

    @Schema(description = "工作空间ID", example = "1234567890123456789")
    private Long id;

    @Schema(description = "关联用户ID", example = "1234567890123456789")
    private Long userId;

    @Schema(description = "工作空间名称", example = "我的工作空间")
    private String name;

    @Schema(description = "工作空间描述", example = "用于日常开发的工作空间")
    private String description;

    @Schema(description = "是否默认工作空间：1是 0否", example = "1")
    private Integer isDefault;

    @Schema(description = "状态: 1正常 2禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}