package com.noah.superagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SpringDoc OpenAPI 3 配置
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    private final OpenApiDocProperties openApiDocProperties;

    public OpenApiConfig(OpenApiDocProperties openApiDocProperties) {
        this.openApiDocProperties = openApiDocProperties;
    }

    /**
     * 创建OpenAPI配置
     * 
     * @return OpenAPI实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenApiDocProperties.Info infoConfig = openApiDocProperties.getInfo();
        OpenApiDocProperties.Contact contactConfig = infoConfig.getContact();
        OpenApiDocProperties.License licenseConfig = infoConfig.getLicense();
        OpenApiDocProperties.ExternalDocs externalDocsConfig = openApiDocProperties.getExternalDocs();

        // 构建API基本信息
        Info info = new Info()
                .title(infoConfig.getTitle())
                .description(infoConfig.getDescription())
                .version(infoConfig.getVersion())
                .termsOfService(infoConfig.getTermsOfService())
                .contact(new Contact()
                        .name(contactConfig.getName())
                        .email(contactConfig.getEmail())
                        .url(contactConfig.getUrl()))
                .license(new License()
                        .name(licenseConfig.getName())
                        .url(licenseConfig.getUrl()));

        // 构建服务器列表
        List<Server> servers = openApiDocProperties.getServers().stream()
                .map(serverConfig -> new Server()
                        .url(serverConfig.getUrl())
                        .description(serverConfig.getDescription()))
                .collect(Collectors.toList());
        
        // 构建外部文档
        ExternalDocumentation externalDocs = new ExternalDocumentation()
                .description(externalDocsConfig.getDescription())
                .url(externalDocsConfig.getUrl());

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .externalDocs(externalDocs)
                .components(new Components()
                        // 添加Cookie认证方案 - satoken
                        .addSecuritySchemes("satoken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("satoken")
                                .description("SSO认证Token，请输入您的satoken值"))
                        // 添加Bearer Token认证方案（备用）
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Bearer Token认证")))
                // 设置全局安全要求
                .addSecurityItem(new SecurityRequirement()
                        .addList("satoken")
                        .addList("bearerAuth"));
    }
}
