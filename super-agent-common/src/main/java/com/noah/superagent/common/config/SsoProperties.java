package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SSO单点登录配置属性
 * 对应 application-sso.yml 中的 sa-token.sso 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "sa-token.sso")
public class SsoProperties {

    /**
     * sso-server后端接口地址
     */
    private String serverUrl;

    /**
     * 业务系统标识
     */
    private String servicecode = "super_agent";

    /**
     * 加密用户登录密码的SM2公钥
     */
    private String sm2Key;

}
