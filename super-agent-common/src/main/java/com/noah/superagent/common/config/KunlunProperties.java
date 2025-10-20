package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kunlun平台配置属性
 * 对应 application.yml 中的 kunlun 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "kunlun")
public class KunlunProperties {

    /**
     * 能力中心编码配置
     */
    private AbilityCodes abilityCodes = new AbilityCodes();

    /**
     * 聊天历史配置
     */
    private ChatHistory chatHistory = new ChatHistory();

    /**
     * A2A通信配置
     */
    private A2a a2a = new A2a();

    /**
     * 注册回调配置
     */
    private Registration registration = new Registration();

    /**
     * 文件存储配置
     */
    private FileRepository fileRepository = new FileRepository();

    /**
     * 能力中心编码配置
     */
    @Data
    public static class AbilityCodes {
        /**
         * 默认能力中心编码
         */
        private String defaultCode;
    }


    /**
     * 聊天历史配置
     */
    @Data
    public static class ChatHistory {
        /**
         * 聊天历史详情URL
         */
        private String url;

        /**
         * 聊天历史列表URL
         */
        private String listUrl;

        /**
         * 聊天历史删除URL
         */
        private String deleteUrl;
    }

    /**
     * A2A通信配置
     */
    @Data
    public static class A2a {
        /**
         * 会话执行接口配置
         */
        private SessionExecution sessionExecution = new SessionExecution();

        /**
         * 补充信息接口配置
         */
        private SupplementInfo supplementInfo = new SupplementInfo();
        
        /**
         * 任务状态获取接口配置
         */
        private TaskStatus taskStatus = new TaskStatus();


        /**
         * 任务状态获取接口配置
         */
        private CancelTask cancelTask = new CancelTask();

        /**
         * 会话执行接口配置
         */
        @Data
        public static class SessionExecution {
            /**
             * 会话执行URL
             */
            private String url;
        }

        /**
         * 补充信息接口配置
         */
        @Data
        public static class SupplementInfo {
            /**
             * 补充信息URL
             */
            private String url;
        }
        
        /**
         * 任务状态获取接口配置
         */
        @Data
        public static class TaskStatus {
            /**
             * 任务状态获取URL
             */
            private String url;
        }

        /**
         * 任务状态获取接口配置
         */
        @Data
        public static class CancelTask {
            /**
             * 取消任务URL
             */
            private String url;
        }

    }

    /**
     * 注册回调配置
     */
    @Data
    public static class Registration {
        /**
         * 回调URL
         */
        private String callbackUrl;
    }

    /**
     * 文件存储配置
     */
    @Data
    public static class FileRepository {

        /**
         * 文件上传路径
         */
        private String uploadPath;

        /**
         * 列出对象名称路径
         */
        private String listObjectNamesPath;

        /**
         * 获取对象URL路径
         */
        private String getObjectUrlPath;
    }

}