package com.noah.superagent.resolver;

import com.noah.superagent.annotation.SaToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * SaToken 参数解析器
 * <p>
 * 处理 @SaToken 注解的方法参数，自动从请求头或 cookie 中获取 satoken 值
 * <p>
 * 获取优先级：
 * 1. 请求头 Satoken
 * 2. cookie satoken
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
public class SaTokenArgumentResolver implements HandlerMethodArgumentResolver {
    
    private static final String SATOKEN_HEADER_NAME = "Satoken";
    private static final String SATOKEN_COOKIE_NAME = "satoken";
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(SaToken.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        
        SaToken annotation = parameter.getParameterAnnotation(SaToken.class);
        if (annotation == null) {
            return null;
        }
        
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            log.warn("无法获取 HttpServletRequest，返回默认值");
            return handleNotFound(annotation, parameter);
        }
        
        // 1. 优先从请求头获取
        String satoken = request.getHeader(SATOKEN_HEADER_NAME);
        if (StringUtils.hasText(satoken)) {
            log.debug("从请求头获取到 satoken: {}", satoken);
            return satoken;
        }
        
        // 2. 从 cookie 中获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SATOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    String cookieValue = cookie.getValue();
                    if (StringUtils.hasText(cookieValue)) {
                        log.debug("从 cookie 获取到 satoken: {}", cookieValue);
                        return cookieValue;
                    }
                }
            }
        }
        
        log.debug("未能获取到 satoken，请求头和 cookie 都为空");
        return handleNotFound(annotation, parameter);
    }
    
    /**
     * 处理未找到 satoken 的情况
     */
    private Object handleNotFound(SaToken annotation, MethodParameter parameter) {
        if (annotation.required()) {
            String parameterName = parameter.getParameterName();
            String methodName = parameter.getMethod() != null ? parameter.getMethod().getName() : "unknown";
            String className = parameter.getDeclaringClass().getSimpleName();
            
            throw new IllegalArgumentException(
                String.format("方法 %s.%s 的参数 %s 标记为必需，但未能从请求头或 cookie 中获取到 satoken", 
                    className, methodName, parameterName));
        }
        
        // 如果不是必需的，返回默认值
        String defaultValue = annotation.defaultValue();
        return StringUtils.hasText(defaultValue) ? defaultValue : null;
    }
}
