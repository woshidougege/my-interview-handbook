package com.noah.superagent.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置类
 */
@Configuration
@ConditionalOnClass(WxPayService.class)
@RequiredArgsConstructor
public class WxPayConfiguration {

    private final WxPayProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public WxPayService wxPayService() {
        WxPayConfig payConfig = new WxPayConfig();
        // V3 API 基本配置
        payConfig.setAppId(StringUtils.trimToNull(properties.getAppId()));
        payConfig.setMchId(StringUtils.trimToNull(properties.getMchId()));
        
        // V3 API 专用配置
        payConfig.setApiV3Key(StringUtils.trimToNull(properties.getApiV3Key()));
        payConfig.setCertSerialNo(StringUtils.trimToNull(properties.getCertSerialNo()));
        payConfig.setPrivateCertPath(StringUtils.trimToNull(properties.getPrivateCertPath()));
        payConfig.setPrivateKeyPath(StringUtils.trimToNull(properties.getPrivateKeyPath()));
        payConfig.setPublicKeyPath(StringUtils.trimToNull(properties.getPublicKeyPath()));
        payConfig.setPublicKeyId(StringUtils.trimToNull(properties.getPublicKeyId()));
        payConfig.setNotifyUrl(StringUtils.trimToNull(properties.getNotifyUrl()));
        
        // 不使用沙箱环境
        payConfig.setUseSandboxEnv(false);

        WxPayService wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(payConfig);
        return wxPayService;
    }
}
