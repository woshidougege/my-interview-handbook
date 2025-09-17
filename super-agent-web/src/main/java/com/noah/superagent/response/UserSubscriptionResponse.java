package com.noah.superagent.response;

import com.noah.superagent.model.UserSubscriptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户订阅信息响应（仅包含订阅信息，不包含积分数据）
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Schema(
    name = "UserSubscriptionResponse",
    title = "用户订阅信息响应",
    description = "仅包含用户订阅信息，积分信息通过独立接口获取"
)
public class UserSubscriptionResponse {

    @Schema(
        description = "用户当前有效订阅信息，如果用户没有活跃订阅则为null", 
        nullable = true
    )
    private UserSubscriptionDTO subscription;

    @Schema(
        description = "套餐名称", 
        example = "基础版"
    )
    private String planName;

    @Schema(
        description = "套餐代码（英文标识）", 
        example = "basic"
    )
    private String planCode;
}
