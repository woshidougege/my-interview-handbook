package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户响应DTO - SSO集成版
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户信息响应（来自SSO）")
public class UserResponse extends BaseResponse {

    @Schema(description = "SSO用户ID", example = "1001")
    private Integer userId;

    @Schema(description = "用户名", example = "张三")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "组织ID", example = "org001")
    private String orgId;

    @Schema(description = "组织名称", example = "技术部")
    private String orgName;

    @Schema(description = "用户状态", example = "1")
    private Integer status;

    @Schema(description = "用户状态描述", example = "正常")
    private String statusDesc;

}
