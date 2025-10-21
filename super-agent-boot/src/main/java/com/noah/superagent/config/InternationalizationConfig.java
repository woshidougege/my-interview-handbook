package com.noah.superagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Arrays;
import java.util.Locale;

/**
 * 国际化配置
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class InternationalizationConfig implements WebMvcConfigurer {

    /**
     * 配置LocaleResolver - 从HTTP请求头Accept-Language获取语言
     * 这是最标准的RESTful方式
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        
        // 设置默认语言为中文
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        
        // 设置支持的语言列表（支持多种语言标识）
        resolver.setSupportedLocales(Arrays.asList(
            Locale.SIMPLIFIED_CHINESE,  // zh-CN, zh
            Locale.CHINESE,              // zh (通用中文)
            Locale.US,                   // en-US
            Locale.ENGLISH               // en (通用英文)
        ));
        
        return resolver;
    }

    /**
     * 配置语言切换拦截器（可选）
     * 如果需要支持URL参数方式切换语言（如 ?lang=en），可以启用此配置
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        // URL参数名称
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}

