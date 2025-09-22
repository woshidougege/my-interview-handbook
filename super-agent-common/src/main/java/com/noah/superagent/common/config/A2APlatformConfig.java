package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A2A平台配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "a2a.platform")
public class A2APlatformConfig {
    
    /**
     * A2A平台基础URL
     */
    private String baseUrl;
    
    /**
     * 能力中心编码映射
     */
    private Map<String, String> abilityCodes;
    
    /**
     * 实体编码映射
     */
    private Map<String, String> entityCodes;
    
    /**
     * 获取默认能力中心编码
     * @return 默认能力中心编码
     */
    public String getDefaultAbilityCode() {
        return abilityCodes != null ? abilityCodes.get("default") : null;
    }
    
    /**
     * 获取默认实体编码
     * @return 默认实体编码
     */
    public String getDefaultEntityCode() {
        return entityCodes != null ? entityCodes.get("default") : null;
    }
    
    /**
     * 根据键获取能力中心编码
     * @param key 键
     * @return 能力中心编码
     */
    public String getAbilityCode(String key) {
        return abilityCodes != null ? abilityCodes.get(key) : null;
    }
    
    /**
     * 根据键获取实体编码
     * @param key 键
     * @return 实体编码
     */
    public String getEntityCode(String key) {
        return entityCodes != null ? entityCodes.get(key) : null;
    }
}