package com.noah.superagent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SaToken 参数注解
 * 用于自动注入当前请求的 satoken 值
 * 
 * 优先从请求头 Satoken 获取，如果没有则从 cookie 中获取
 * 
 * 使用示例：
 * <pre>
 * public ResponseEntity<StreamingResponseBody> someMethod(
 *         &#64;RequestBody SomeRequest request,
 *         &#64;SaToken String satoken) {
 *     // 直接使用 satoken
 * }
 * </pre>
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SaToken {
    
    /**
     * 是否必需
     * 如果为 true 且获取不到 satoken，将抛出异常
     * 如果为 false 且获取不到 satoken，将返回 null
     * 
     * @return 是否必需，默认为 false
     */
    boolean required() default false;
    
    /**
     * 当获取不到 satoken 时的默认值
     * 只有在 required = false 时生效
     * 
     * @return 默认值
     */
    String defaultValue() default "";
}
