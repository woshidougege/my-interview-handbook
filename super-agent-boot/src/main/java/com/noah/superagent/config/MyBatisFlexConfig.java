package com.noah.superagent.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Flex 配置
 *
 * @author System
 * @since 1.0.0
 */
@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 开启审计功能（可选）
        AuditManager.setAuditEnable(true);
        
        // 雪花算法ID生成器已内置，无需额外配置
        // 直接在实体类使用: @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    }
}
