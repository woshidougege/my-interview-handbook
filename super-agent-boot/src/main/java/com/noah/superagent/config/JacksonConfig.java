package com.noah.superagent.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson JSON序列化配置
 * 主要解决前端JavaScript精度丢失问题
 *
 * @author System
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置Long类型序列化为String
     * 解决前端JavaScript Number类型精度丢失问题
     * <p>
     * JavaScript Number类型只能安全表示到2^53-1（约16位）的整数，
     * 而雪花算法生成的ID可能达到19位，会导致前端精度丢失。
     * <p>
     * 通过此配置，所有Long类型字段在JSON序列化时会转为String类型。
     */
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        
        // 配置日期格式
        builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 配置Long类型序列化为String
        builder.serializerByType(Long.class, ToStringSerializer.instance);
        builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        
        return builder;
    }
}
