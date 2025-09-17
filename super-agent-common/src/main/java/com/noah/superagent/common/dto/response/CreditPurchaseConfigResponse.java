package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分购买配置响应数据
 */
@Data
@Schema(description = "积分购买配置响应数据")
public class CreditPurchaseConfigResponse {

    @Schema(description = "当前套餐名称", example = "基础版")
    private String planName;

    @Schema(description = "当前套餐代码", example = "basic")
    private String planCode;

    @Schema(description = "积分有效期天数，0表示永久有效", example = "0")
    private Integer creditValidityDays;

    @Schema(description = "可用积分总数", example = "3200")
    private String availableCredits;

    @Schema(description = "每日刷新积分", example = "300")
    private String dailyRefreshCredits;

    @Schema(description = "限时积分", example = "0")
    private String limitedCredits;

    @Schema(description = "积分套餐配置列表")
    private List<CreditPackageConfig> creditPackages;

    /**
     * 积分套餐配置
     */
    @Data
    @Schema(description = "积分套餐配置")
    public static class CreditPackageConfig {

        @Schema(description = "套餐ID", example = "100")
        private String id;

        @Schema(description = "套餐名称", example = "10000积分")
        private String name;

        @Schema(description = "套餐代码", example = "CREDIT_PACK_10000")
        private String code;

        @Schema(description = "积分数量", example = "10000")
        private Long creditsAmount;

        @Schema(description = "价格", example = "59")
        private BigDecimal price;

        @Schema(description = "是否推荐", example = "false")
        private Boolean isRecommended;

        @Schema(description = "功能特性列表")
        private List<FeatureConfig> features;
    }

    /**
     * 功能特性配置
     */
    @Data
    @Schema(description = "功能特性配置")
    public static class FeatureConfig {

        @Schema(description = "功能描述文本", example = "约生成10-14个PPT")
        private String text;

        @Schema(description = "是否高亮显示", example = "false")
        private Boolean highlight;
    }
}
