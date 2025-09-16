package com.noah.superagent.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON序列化配置
 * 主要解决前端JavaScript精度丢失问题
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Jackson序列化配置
     * <p>
     * 1. 解决前端JavaScript Number类型精度丢失问题 - Long类型序列化为String
     * 2. 配置Java 8时间类型格式 - LocalDateTime等使用标准格式
     * <p>
     * JavaScript Number类型只能安全表示到2^53-1（约16位）的整数，
     * 而雪花算法生成的ID可能达到19位，会导致前端精度丢失。
     */
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        
        // 配置传统Date类型格式
        builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 配置Java 8时间类型格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
        builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        
        // 配置Long类型序列化为String
        builder.serializerByType(Long.class, ToStringSerializer.instance);
        builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        
        return builder;
    }
}
