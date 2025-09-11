package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PasswordLoginRequest {
    @Schema(description = "用户名", required = true, example = "admin")
    private String username;

    @Schema(description = "SM2加密后的密码", required = true, example = "304802...")
    private String pwd;

    @Schema(description = "服务代码", required = true, example = "super_agent")
    private String servicecode;
}