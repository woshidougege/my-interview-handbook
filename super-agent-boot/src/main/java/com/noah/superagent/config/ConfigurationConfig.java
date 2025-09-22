package com.noah.superagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.noah.superagent.common.config.BillingProperties;
import com.noah.superagent.common.config.AsyncProperties;
import com.noah.superagent.common.config.PlansConfig;
import com.noah.superagent.common.config.AiProperties;

/**
 * 统一配置管理类
 * 将所有@ConfigurationProperties集中管理，保持启动类的简洁
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({
    OpenApiDocProperties.class,
    WxPayProperties.class,
    BillingProperties.class,
    AsyncProperties.class,
    PlansConfig.class,
    AiProperties.class
    // 未来有新的配置类时，只需要在这里添加
})
public class ConfigurationConfig {
    
    /*
      这个类不需要任何实现，只是为了集中管理配置类

      优势：
      1. 启动类保持简洁
      2. 配置类集中管理，易于维护
      3. 新增配置时只需修改这一个地方
      4. 配置职责单一，符合单一职责原则
     */
}
