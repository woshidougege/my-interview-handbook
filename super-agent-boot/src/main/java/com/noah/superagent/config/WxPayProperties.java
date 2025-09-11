package com.noah.superagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信支付配置属性
 */
@Data
@ConfigurationProperties(prefix = "wx.pay")
public class WxPayProperties {
    
    /**
     * 设置微信公众号或者小程序等的appid
     */
    private String appId;

    /**
     * 微信支付商户号
     */
    private String mchId;

    // V2 API配置已移除，只保留V3 API需要的配置

    /**
     * apiV3 秘钥值
     */
    private String apiV3Key;

    /**
     * apiV3 证书序列号值
     */
    private String certSerialNo;

    /**
     * apiclient_cert.pem证书文件的绝对路径或者以classpath:开头的类路径
     */
    private String privateCertPath;

    /**
     * apiclient_key.pem证书文件的绝对路径或者以classpath:开头的类路径
     */
    private String privateKeyPath;

    /**
     * 微信支付公钥，pub_key.pem证书文件的绝对路径或者以classpath:开头的类路径
     */
    private String publicKeyPath;

    /**
     * 微信支付公钥ID
     */
    private String publicKeyId;

    /**
     * 支付成功回调地址
     */
    private String notifyUrl;
}
