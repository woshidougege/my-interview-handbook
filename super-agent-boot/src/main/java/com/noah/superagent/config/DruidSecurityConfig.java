package com.noah.superagent.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Druid 安全配置类
 * 专门处理 PostgreSQL 数据库的 WallFilter 配置问题
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.datasource.druid.wall.enabled", havingValue = "true")
public class DruidSecurityConfig {

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    /**
     * 创建针对 PostgreSQL 优化的 StatFilter
     * 解决 PostgreSQL 函数语法解析错误问题
     */
    @Bean
    public StatFilter statFilter() {
        StatFilter statFilter = new StatFilter();
        
        // 检测是否为 PostgreSQL
        boolean isPostgreSQL = driverClassName.contains("postgresql");
        
        if (isPostgreSQL) {
            log.info("检测到 PostgreSQL 数据库，配置 StatFilter 以避免函数语法解析错误");
            // 禁用SQL合并功能，避免解析复杂的PostgreSQL语法
            statFilter.setMergeSql(false);
            statFilter.setLogSlowSql(true);
            statFilter.setSlowSqlMillis(2000);
        } else {
            // 默认配置
            statFilter.setMergeSql(true);
            statFilter.setLogSlowSql(true);
            statFilter.setSlowSqlMillis(2000);
        }
        
        return statFilter;
    }

    /**
     * 创建针对 PostgreSQL 优化的 WallFilter
     * 解决 PostgreSQL 函数创建语法不被识别的问题
     */
    @Bean
    public WallFilter wallFilter() {
        WallFilter wallFilter = new WallFilter();
        
        // 创建 WallConfig
        WallConfig wallConfig = new WallConfig();
        
        // 检测是否为 PostgreSQL
        boolean isPostgreSQL = driverClassName.contains("postgresql");
        
        if (isPostgreSQL) {
            log.info("检测到 PostgreSQL 数据库，应用 PostgreSQL 特定的 WallFilter 配置");
            configureForPostgreSQL(wallConfig);
        } else {
            log.info("应用默认的 WallFilter 配置");
            configureDefault(wallConfig);
        }
        
        wallFilter.setConfig(wallConfig);
        return wallFilter;
    }

    /**
     * PostgreSQL 特定配置
     */
    private void configureForPostgreSQL(WallConfig config) {
        // 基础安全配置
        config.setMultiStatementAllow(true);
        config.setNoneBaseStatementAllow(true);
        config.setCommentAllow(true);
        
        // 禁用函数检查（解决 $$ 语法问题）
        config.setFunctionCheck(false);
        
        // 禁用严格语法检查（支持复杂 PL/pgSQL 语法）
        config.setStrictSyntaxCheck(false);
        
        // DDL 操作权限
        config.setCreateTableAllow(true);
        config.setAlterTableAllow(true);
        config.setDropTableAllow(true);
        
        // DML 操作权限
        config.setSelectAllow(true);
        config.setInsertAllow(true);
        config.setUpdateAllow(true);
        config.setDeleteAllow(true);
        
        // 限制危险操作
        config.setTruncateAllow(false);
        config.setCallAllow(false);
        config.setSelectIntoAllow(false);
        
        // PostgreSQL 特定权限
        config.setDescribeAllow(true);
        config.setShowAllow(true);
        config.setUseAllow(true);
        
        // 宽松的语法检查（支持 PostgreSQL 特殊语法）
        config.setSchemaCheck(false);
        
        log.info("PostgreSQL WallFilter 配置完成：禁用函数检查，支持 PL/pgSQL 语法");
    }

    /**
     * 默认配置（适用于其他数据库）
     */
    private void configureDefault(WallConfig config) {
        config.setMultiStatementAllow(true);
        config.setCommentAllow(true);
        
        // 基本 DDL 权限
        config.setCreateTableAllow(true);
        config.setAlterTableAllow(true);
        config.setDropTableAllow(false); // 默认不允许删除表
        
        // 基本 DML 权限
        config.setSelectAllow(true);
        config.setInsertAllow(true);
        config.setUpdateAllow(true);
        config.setDeleteAllow(true);
        
        // 限制危险操作
        config.setTruncateAllow(false);
        config.setCallAllow(false);
        
        log.info("默认 WallFilter 配置完成");
    }
}
