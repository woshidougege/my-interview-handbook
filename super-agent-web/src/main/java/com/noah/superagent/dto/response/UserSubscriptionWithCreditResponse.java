package com.noah.superagent.dto.response;

import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户订阅信息和积分数量响应
 * 整合了订阅信息和积分数量，避免多次API调用
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(description = "用户订阅信息和积分数量响应")
public class UserSubscriptionWithCreditResponse {

    @Schema(description = "用户订阅信息")
    private UserSubscriptionEntity subscription;

    @Schema(description = "用户可用积分总数", example = "1500")
    private Long availableCredits;

    @Schema(description = "是否有积分账户", example = "true")
    private Boolean hasCreditAccount;
}
