package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 *
 * @author System
 * @since 1.0.0
 */
@Data
@Schema(description = "用户信息响应")
public class UserResponse {

    @Schema(description = "用户ID", example = "1234567890123456789")
    private Long id;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户状态 1-正常 0-禁用", example = "1")
    private Integer status;

    @Schema(description = "用户状态描述", example = "正常")
    private String statusDesc;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}
