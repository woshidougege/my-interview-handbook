# Super Agent Platform

用户管理和计费平台

## 🚀 快速启动

### 1. 打包应用
```bash
mvn clean package -Drevision=1.0.0
```

### 2. 创建数据库
```sql
CREATE DATABASE super_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 启动应用
```bash
# 解压部署包
tar -xzf super-agent-boot-1.0.0.tar.gz
cd super-agent-boot-1.0.0

# 启动应用（默认开发环境）
./bin/startup.sh start

# 指定环境启动
./bin/startup.sh start dev   # 开发环境
./bin/startup.sh start test  # 测试环境  
./bin/startup.sh start prod  # 生产环境
```

## 📁 目录结构
```
super-agent-boot-1.0.0/
├── bin/                    # 启动脚本
│   ├── startup.sh         # Linux/Mac启动脚本
│   └── startup.bat        # Windows启动脚本
├── conf/                  # 配置文件
│   ├── application.yml         # 公共配置
│   ├── application-dev.yml     # 开发环境
│   ├── application-test.yml    # 测试环境
│   └── application-prod.yml    # 生产环境
├── lib/                   # JAR包
│   └── super-agent-boot.jar
└── logs/                  # 日志目录
```

## 🔧 环境配置

### 开发环境 (dev)
```bash
# 自动创建数据库表
# 启用监控页面和API文档
# 详细的DEBUG日志
./bin/startup.sh start dev
```

### 测试环境 (test)
```bash
# 需要配置测试数据库
export DB_USERNAME=test_user
export DB_PASSWORD=test_pass
./bin/startup.sh start test
```

### 生产环境 (prod)
```bash
# 必须设置环境变量
export DB_URL="jdbc:mysql://prod-db:3306/super_agent"
export DB_USERNAME="app_user"
export DB_PASSWORD="secure_password"
./bin/startup.sh start prod
```

## 📋 管理命令
```bash
./bin/startup.sh start [profile]    # 启动
./bin/startup.sh stop               # 停止
./bin/startup.sh restart [profile]  # 重启
./bin/startup.sh status             # 状态
```

## 📖 访问地址

### 开发/测试环境
- **应用**: http://localhost:8080
- **API文档**: http://localhost:8080/doc.html
- **监控页面**: http://localhost:8080/druid (admin/admin)

### 生产环境
- **应用**: http://server:port
- **API文档**: 默认关闭（可通过环境变量启用）
- **监控页面**: 已关闭

## 🔧 环境要求
- **JDK**: 17+
- **MySQL**: 8.0+
- **内存**: 最少512MB，推荐1GB+
