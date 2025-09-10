package com.noah.superagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 在应用启动早期，输出 Spring Boot 加载到的配置资源列表。
 * 目的：在不打开全局 TRACE 的情况下，直观看到配置拆分是否被正确导入。
 */
public class ConfigLoadLogger implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoadLogger.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();

        Set<String> loaded = new LinkedHashSet<>();
        // 限制遍历数量，避免在PropertySources很多时耗时过长
        int count = 0;
        for (PropertySource<?> ps : environment.getPropertySources()) {
            if (++count > 30) break; // 早期配置检查，限制更严格
            
            String name = ps.getName();
            // 典型名称示例：
            //  Config resource 'class path resource [application.yml]' via location 'optional:classpath:/'
            //  Config resource 'class path resource [application-logging.yml]' via location 'classpath:application-logging.yml'
            //  过滤出与 application*.yml 相关的 PropertySource
            if (name.startsWith("Config resource '") && (name.contains("application.yml")
                    || name.contains("application.yaml")
                    || name.contains("application-"))) {
                loaded.add(name);
            }
        }

        if (!loaded.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n================= CONFIG FILES LOADED =================\n");
            loaded.forEach(n -> sb.append("  - ").append(n).append('\n'));
            sb.append("======================================================\n");
            log.info(sb.toString());
        }
    }

    // 越小越早，这里保持默认顺序即可
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
