package com.noah.superagent.config;

import org.killbill.billing.client.KillBillHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kill Bill 客户端配置
 *
 * @author Noah
 * @since 1.0.0
 */
@Configuration
public class KillBillConfig {

    @Value("${killbill.server.url:http://localhost:8080}")
    private String killBillUrl;

    @Value("${killbill.server.username:admin}")
    private String username;

    @Value("${killbill.server.password:password}")
    private String password;

    @Value("${killbill.server.api-key:bob}")
    private String apiKey;

    @Value("${killbill.server.api-secret:lazar}")
    private String apiSecret;

    @Bean
    public KillBillHttpClient killBillHttpClient() {
        return new KillBillHttpClient(
            killBillUrl,
            username,
            password,
            apiKey,
            apiSecret
        );
    }
}
