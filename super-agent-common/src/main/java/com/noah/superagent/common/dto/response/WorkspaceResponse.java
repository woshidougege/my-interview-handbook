package com.noah.superagent.common.dto.response;

import com.noah.superagent.common.enums.DefaultEnum;
import com.noah.superagent.common.enums.WorkspaceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作空间响应DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作空间信息响应")
public class WorkspaceResponse extends BaseResponse {

    @Schema(description = "关联用户ID", example = "1234567890123456789")
    private Long userId;

    @Schema(description = "工作空间名称", example = "我的工作空间")
    private String name;

    @Schema(description = "工作空间描述", example = "用于日常开发的工作空间")
    private String description;

    @Schema(description = "是否默认工作空间：DEFAULT-默认 NOT_DEFAULT-非默认", example = "DEFAULT")
    private DefaultEnum isDefault;

    @Schema(description = "状态: NORMAL-正常 DISABLED-禁用", example = "NORMAL")
    private WorkspaceStatusEnum status;

}