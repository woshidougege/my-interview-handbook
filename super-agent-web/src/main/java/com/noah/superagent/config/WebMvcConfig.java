package com.noah.superagent.config;

import com.noah.superagent.resolver.SaTokenArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置超时时间为5分钟（300000毫秒）
        configurer.setDefaultTimeout(300000);
        // 也可以配置自定义的线程池
        // configurer.setTaskExecutor(taskExecutor());
    }
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 注册 SaToken 参数解析器
        resolvers.add(new SaTokenArgumentResolver());
    }
}
