# Super Agent Platform

用户管理和计费平台

## 🚀 部署说明

### 1. 打包
```bash
mvn clean package -Drevision=1.0.0
```

### 2. 部署结构
打包后会生成标准应用目录：
```
super-agent-boot-1.0.0/
├── bin/                    # 启动脚本
│   ├── startup.sh         # Linux/Mac启动脚本
│   └── startup.bat        # Windows启动脚本
├── conf/                  # 配置文件
│   └── application.yml    # 应用配置
├── lib/                   # JAR包
│   └── super-agent-boot.jar
└── logs/                  # 日志目录
```

### 3. 启动应用
```bash
# Linux/Mac
cd super-agent-boot-1.0.0
./bin/startup.sh start

# Windows  
cd super-agent-boot-1.0.0
bin\startup.bat start
```

### 4. 管理命令
```bash
./bin/startup.sh start    # 启动
./bin/startup.sh stop     # 停止
./bin/startup.sh restart  # 重启
./bin/startup.sh status   # 状态
```

## 📖 访问地址
- **API文档**: http://localhost:8080/doc.html
- **监控页面**: http://localhost:8080/druid (admin/admin)

## 🔧 环境要求
- JDK 17+
- MySQL 8.0+
