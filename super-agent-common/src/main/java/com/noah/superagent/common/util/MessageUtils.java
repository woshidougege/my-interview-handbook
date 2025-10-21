package com.noah.superagent.common.util;

import com.noah.superagent.common.enums.MessageKeyEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MessageUtils {

    private final MessageSource messageSource;

    /**
     * 根据消息键枚举获取国际化消息（推荐使用）
     * 
     * @param messageKey 消息键枚举
     * @return 国际化消息
     */
    public String getMessage(MessageKeyEnum messageKey) {
        return getMessage(messageKey.getKey(), null);
    }

    /**
     * 根据消息键枚举和参数获取国际化消息（推荐使用）
     * 
     * @param messageKey 消息键枚举
     * @param args 参数数组
     * @return 国际化消息
     */
    public String getMessage(MessageKeyEnum messageKey, Object[] args) {
        return getMessage(messageKey.getKey(), args);
    }

    /**
     * 根据消息键获取国际化消息
     * 
     * @param key 消息键
     * @return 国际化消息
     */
    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /**
     * 根据消息键和参数获取国际化消息
     * 
     * @param key 消息键
     * @param args 参数数组
     * @return 国际化消息
     */
    public String getMessage(String key, Object[] args) {
        return getMessage(key, args, key);
    }

    /**
     * 根据消息键、参数和默认消息获取国际化消息
     * 
     * @param key 消息键
     * @param args 参数数组
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public String getMessage(String key, Object[] args, String defaultMessage) {
        try {
            // 从当前线程上下文获取Locale（由LocaleResolver自动设置）
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, args, defaultMessage, locale);
        } catch (Exception e) {
            return defaultMessage;
        }
    }

    /**
     * 获取指定语言的消息
     * 
     * @param key 消息键
     * @param args 参数数组
     * @param locale 语言环境
     * @return 国际化消息
     */
    public String getMessage(String key, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(key, args, key, locale);
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * 根据套餐代码获取国际化的套餐名称
     * 
     * @param planCode 套餐代码（如 FREE、BASIC、PREMIUM）
     * @return 国际化的套餐名称
     */
    public String getPlanName(String planCode) {
        if (planCode == null || planCode.isEmpty()) {
            return "";
        }
        // 动态构建消息key：plan.{PLAN_CODE}.name
        String messageKey = "plan." + planCode + ".name";
        return getMessage(messageKey, null, planCode);
    }

    /**
     * 根据套餐代码获取国际化的套餐描述
     * 
     * @param planCode 套餐代码（如 FREE、BASIC、PREMIUM）
     * @return 国际化的套餐描述
     */
    public String getPlanDescription(String planCode) {
        if (planCode == null || planCode.isEmpty()) {
            return "";
        }
        // 动态构建消息key：plan.{PLAN_CODE}.description
        String messageKey = "plan." + planCode + ".description";
        return getMessage(messageKey, null, "");
    }

    /**
     * 根据套餐代码和功能key获取国际化的功能描述
     * 
     * @param planCode 套餐代码（如 FREE、BASIC、PREMIUM）
     * @param featureKey 功能key（如 daily_credits、access_chat）
     * @return 国际化的功能描述
     */
    public String getPlanFeature(String planCode, String featureKey) {
        if (planCode == null || planCode.isEmpty() || featureKey == null || featureKey.isEmpty()) {
            return "";
        }
        // 动态构建消息key：plan.{PLAN_CODE}.feature.{FEATURE_KEY}
        String messageKey = "plan." + planCode + ".feature." + featureKey;
        return getMessage(messageKey, null, featureKey);
    }
}

