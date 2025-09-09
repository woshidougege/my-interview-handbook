package com.noah.superagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 异步异常处理配置
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class AsyncExceptionHandler implements AsyncConfigurer {

    /**
     * 获取异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        // 使用默认执行器，具体执行器在方法级别通过@Async指定
        return null;
    }

    /**
     * 处理异步方法中未捕获的异常
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new TokenReportAsyncExceptionHandler();
    }

    /**
     * Token上报异步异常处理器
     */
    public static class TokenReportAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
            log.error("异步方法执行失败 - 方法: {}.{}, 参数数量: {}, 异常信息: {}", 
                method.getDeclaringClass().getSimpleName(), 
                method.getName(), 
                params != null ? params.length : 0, 
                throwable.getMessage(), 
                throwable);

            // 如果是Token上报相关的异常，可以进行特殊处理
            if (method.getName().contains("recordTokenUsageAsync")) {
                handleTokenReportException(throwable, params);
            }
        }

        /**
         * 处理Token上报异常
         */
        private void handleTokenReportException(Throwable throwable, Object... params) {
            try {
                if (params != null && params.length > 0) {
                    // 尝试从参数中获取请求信息
                    Object firstParam = params[0];
                    log.error("Token上报异步处理失败，建议检查数据库连接和线程池配置 - 参数类型: {}, 异常: {}", 
                        firstParam != null ? firstParam.getClass().getSimpleName() : "null", 
                        throwable.getMessage());
                }
                
                // 这里可以添加额外的处理逻辑，比如：
                // 1. 发送告警通知
                // 2. 记录到错误日志表
                // 3. 触发重试机制
                // 4. 更新监控指标
                
            } catch (Exception e) {
                log.error("处理Token上报异步异常时发生错误: {}", e.getMessage(), e);
            }
        }
    }
}
