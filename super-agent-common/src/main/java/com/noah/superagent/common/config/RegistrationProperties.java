package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 注册配置属性
 * 对应 application.yml 中的 registration 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "registration")
public class RegistrationProperties {

    /**
     * 回调URL配置
     */
    private CallbackConfig callback = new CallbackConfig();

    @Data
    public static class CallbackConfig {
        /**
         * 注册回调URL
         */
        private String url;
    }
}
