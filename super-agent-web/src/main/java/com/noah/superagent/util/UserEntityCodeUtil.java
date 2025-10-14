package com.noah.superagent.util;

import com.noah.superagent.model.SSOUserInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户实体编码工具类
 * 负责生成和管理用户实体编码
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
public class UserEntityCodeUtil {

    /**
     * 根据用户信息生成用户实体编码
     *
     * @param user 用户信息
     * @return 用户实体编码，如果用户信息不完整返回null
     */
    public static String generateUserEntityCode(SSOUserInfo user) {
        if (user == null) {
            log.error("用户信息为空");
            return null;
        }

        String userId = user.getUserId();
        String phonenumber = user.getPhonenumber();

        // 验证必要用户信息
        if (userId == null || userId.isEmpty()) {
            log.error("用户ID为空");
            return null;
        }

        // 构建用户实体编码
        return "ENTITY_document_" + userId + "_" + (phonenumber != null ? phonenumber : "");
    }
    /**
     * 根据用户信息生成用户实体编码
     *
     * @param user 用户信息
     * @return 用户实体编码，如果用户信息不完整返回null
     */
    public static String generateAgentEntityCode(SSOUserInfo user) {
        if (user == null) {
            log.error("用户信息为空");
            return null;
        }

        String userId = user.getUserId();
        String phonenumber = user.getPhonenumber();

        // 验证必要用户信息
        if (userId == null || userId.isEmpty()) {
            log.error("用户ID为空");
            return null;
        }

        // 构建用户实体编码
        return "ENTITY_agent_" + userId + "_" + phonenumber;
    }
    /**
     * 获取当前用户的实体编码
     *
     * @return 当前用户的实体编码，如果获取失败返回null
     */
    public static String getCurrentUserEntityCode() {
        SSOUserInfo currentUser = UserContext.getCurrentUser();
        return generateUserEntityCode(currentUser);
    }


    public static String getCurrentUserAgentEntityCode() {
        SSOUserInfo currentUser = UserContext.getCurrentUser();
        return generateAgentEntityCode(currentUser);
    }
}
