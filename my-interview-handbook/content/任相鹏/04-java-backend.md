---
title: "Java后端"
icon: "☕"
order: 4
description: "Spring Boot、Dubbo、MyBatis 等Java后端技术"
---

## Spring Boot

### 核心特性

- **自动配置**：约定优于配置
- **起步依赖**：简化依赖管理
- **内嵌服务器**：Tomcat/Jetty
- **生产就绪**：健康检查、指标监控

### 常用注解

- `@SpringBootApplication`：启动类
- `@RestController`：REST 接口
- `@Service`：业务层
- `@Repository`：数据访问层
- `@Autowired`：依赖注入

---

## Dubbo RPC

### [[Dubbo|都宝|阿里开源 RPC 框架]]

**核心功能**

- 服务注册与发现
- 负载均衡
- 容错机制
- 服务降级

---

## MyBatis

### [[MyBatis|迈百踢斯|持久层框架]]

**核心特点**

- SQL 与 Java 分离
- 灵活的映射配置
- 支持动态 SQL
- 二级缓存

---

## 面试常见问题

### Q1：Spring Boot 自动配置原理？

1. `@EnableAutoConfiguration`
2. 扫描 META-INF/spring.factories
3. 根据条件注解（@ConditionalOnClass）
4. 加载对应的配置类

### Q2：如何实现分布式事务？

- TCC（Try-Confirm-Cancel）
- Saga 模式
- 本地消息表
- RocketMQ 事务消息

