# Super Agent Platform

用户管理和计费平台 - 简化版

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### 数据库配置
```sql
-- 创建数据库
CREATE DATABASE super_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 启动应用

**方式1：使用脚本启动**
```bash
# Linux/Mac
./scripts/build.sh

# Windows
scripts\build.bat
```

**方式2：Maven启动**
```bash
# 使用默认版本
mvn spring-boot:run -pl super-agent-boot

# 指定版本
mvn spring-boot:run -pl super-agent-boot -Drevision=1.0.0-RELEASE
```

### 版本管理

使用 `${revision}` 统一管理版本：

```bash
# 打包指定版本
mvn clean package -Drevision=1.0.0-RELEASE

# 检查版本更新
mvn versions:display-dependency-updates
```

## 📖 访问地址

- **应用地址**: http://localhost:8080
- **API文档**: http://localhost:8080/doc.html
- **Druid监控**: http://localhost:8080/druid (admin/admin)

## 🏗️ 项目结构

```
super-agent/
├── super-agent-common/      # 公共模块
├── super-agent-dao/         # 数据访问层
├── super-agent-service-api/ # 业务接口层
├── super-agent-service-impl/# 业务实现层
├── super-agent-web/         # Web接口层
├── super-agent-schedule/    # 定时任务层
└── super-agent-boot/        # 启动模块
```

## 📊 核心功能

- ✅ 用户管理（手机号、昵称、密码）
- ✅ 积分账户管理
- ✅ 包月套餐管理
- ✅ 积分交易记录
- ✅ 定时任务调度

## 🔧 技术栈

- **Spring Boot 3.2** + **JDK 17**
- **MyBatis Flex** + **MySQL 8.0**
- **Druid** 连接池
- **db-scheduler** 定时任务
- **Flyway** 数据库版本管理
- **Knife4j** API文档
