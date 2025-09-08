package com.noah.superagent.common.dto.request;

import com.noah.superagent.common.enums.DefaultEnum;
import com.noah.superagent.common.enums.WorkspaceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 工作空间创建请求DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作空间创建请求")
public class WorkspaceCreateRequest extends BaseRequest {

    @NotNull(message = "关联用户ID不能为空")
    @Schema(description = "关联用户ID", example = "1234567890123456789")
    private Long userId;

    @NotBlank(message = "工作空间名称不能为空")
    @Schema(description = "工作空间名称", example = "我的工作空间")
    private String name;

    @Schema(description = "工作空间描述", example = "用于日常开发的工作空间")
    private String description;

    @Schema(description = "是否默认工作空间：DEFAULT-默认 NOT_DEFAULT-非默认", example = "DEFAULT")
    private DefaultEnum isDefault;

    @Schema(description = "状态: NORMAL-正常 DISABLED-禁用", example = "NORMAL")
    private WorkspaceStatusEnum status;
}