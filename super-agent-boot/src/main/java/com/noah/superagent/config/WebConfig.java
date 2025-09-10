package com.noah.superagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置
 * 支持前端SPA应用的路由处理
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源处理
     * 支持Next.js等SPA应用的前端路由
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // API请求不处理，交给Controller
        registry.addResourceHandler("/api/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false);

        // 前端静态资源处理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 对于不存在的资源，如果不是API请求且不包含文件扩展名，返回index.html
                        // 这样可以支持前端路由（SPA）
                        if (!resourcePath.startsWith("api/") && !resourcePath.contains(".")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        
                        return null;
                    }
                });
    }
}
