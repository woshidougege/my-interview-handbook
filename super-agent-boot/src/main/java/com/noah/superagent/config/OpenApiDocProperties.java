package com.noah.superagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAPI文档配置属性
 * 
 * @author Super Agent Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "springdoc")
public class OpenApiDocProperties {

    private Info info = new Info();
    private List<Server> servers;
    private ExternalDocs externalDocs = new ExternalDocs();

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }
    
    public ExternalDocs getExternalDocs() {
        return externalDocs;
    }

    public void setExternalDocs(ExternalDocs externalDocs) {
        this.externalDocs = externalDocs;
    }

    /**
     * API基本信息
     */
    public static class Info {
        private String title;
        private String description;
        private String version;
        private String termsOfService;
        private Contact contact = new Contact();
        private License license = new License();

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getTermsOfService() { return termsOfService; }
        public void setTermsOfService(String termsOfService) { this.termsOfService = termsOfService; }
        public Contact getContact() { return contact; }
        public void setContact(Contact contact) { this.contact = contact; }
        public License getLicense() { return license; }
        public void setLicense(License license) { this.license = license; }
    }

    /**
     * 联系人信息
     */
    public static class Contact {
        private String name;
        private String email;
        private String url;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    /**
     * 许可证信息
     */
    public static class License {
        private String name;
        private String url;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    /**
     * 服务器信息
     */
    public static class Server {
        private String url;
        private String description;
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 外部文档
     */
    public static class ExternalDocs {
        private String description;
        private String url;
        
        // Getters and Setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
