package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户积分状态响应
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户积分状态响应")
public class UserCreditStatusResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "总积分余额", example = "1250.50")
    private BigDecimal totalBalance;

    @Schema(description = "总已消费积分", example = "750.30")
    private BigDecimal totalSpent;

    @Schema(description = "账户状态", example = "NORMAL", allowableValues = {"NORMAL", "OVERDRAWN", "FROZEN"})
    private String accountStatus;

    @Schema(description = "是否欠费")
    private Boolean isOverdrawn;

    @Schema(description = "欠费金额（负数表示欠费）", example = "-50.00")
    private BigDecimal overdraftAmount;

    @Schema(description = "可透支额度", example = "300.00")
    private BigDecimal overdraftLimit;

    @Schema(description = "剩余可透支额度", example = "250.00")
    private BigDecimal remainingOverdraftLimit;

    @Schema(description = "透支功能是否启用")
    private Boolean overdraftEnabled;

    @Schema(description = "各类型积分详情")
    private List<CreditTypeBalance> creditTypeBalances;

    @Schema(description = "账户创建时间")
    private LocalDateTime accountCreatedTime;

    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;

    /**
     * 积分类型余额详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "积分类型余额详情")
    public static class CreditTypeBalance {
        
        @Schema(description = "积分类型", example = "DAILY", allowableValues = {"DAILY", "ACTIVITY", "FREE", "PAID"})
        private String creditType;

        @Schema(description = "积分类型名称", example = "每日积分")
        private String creditTypeName;

        @Schema(description = "当前余额", example = "100.00")
        private BigDecimal balance;

        @Schema(description = "已消费金额", example = "50.00")
        private BigDecimal spent;

        @Schema(description = "有效期（天数，-1表示永久有效）", example = "1")
        private Integer validityDays;

        @Schema(description = "是否即将过期（3天内过期）")
        private Boolean expiringSoon;

        @Schema(description = "过期时间")
        private LocalDateTime expiryTime;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;
    }

    /**
     * 账户状态枚举
     */
    public enum AccountStatus {
        NORMAL("正常"),
        OVERDRAWN("透支中"), 
        FROZEN("已冻结");

        private final String description;

        AccountStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
