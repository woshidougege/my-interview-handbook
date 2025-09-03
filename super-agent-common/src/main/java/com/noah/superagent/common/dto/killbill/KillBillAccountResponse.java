package com.noah.superagent.common.dto.killbill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kill Bill账户响应
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@Schema(description = "Kill Bill账户响应")
public class KillBillAccountResponse {

    @Schema(description = "账户ID")
    private String accountId;

    @Schema(description = "用户名")
    private String name;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "外部键")
    private String externalKey;

    @Schema(description = "货币")
    private String currency;

    @Schema(description = "账户余额")
    private BigDecimal accountBalance;

    @Schema(description = "创建时间")
    private LocalDateTime createdDate;
}
