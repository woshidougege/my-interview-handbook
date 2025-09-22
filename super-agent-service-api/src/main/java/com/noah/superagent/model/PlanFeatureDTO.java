package com.noah.superagent.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 订阅套餐功能特性DTO
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeatureDTO {

    /**
     * 功能描述文本
     */
    private String text;

    /**
     * 是否高亮显示
     */
    private Boolean highlight;
}
