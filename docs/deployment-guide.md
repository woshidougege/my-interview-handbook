# 部署配置指南

## 📁 配置文件说明

### 1. application.yml - 公共配置
**作用**：所有环境通用的配置
- 服务器基础配置
- 连接池公共参数
- MyBatis、调度器等组件配置
- 使用环境变量占位符：`${VAR:默认值}`

### 2. application-dev.yml - 开发环境
**作用**：开发环境特有配置
- ✅ 启用Flyway自动执行
- ✅ 启用Druid监控页面
- ✅ 详细的DEBUG日志
- ✅ 打印SQL语句
- ✅ 包含开发环境默认值

### 3. application-prod.yml - 生产环境
**作用**：生产环境优化配置
- ❌ 禁用Flyway自动执行
- ❌ 关闭Druid监控页面
- ❌ 关闭API文档（可选）
- ✅ 优化连接池参数
- ✅ 日志滚动和压缩
- ✅ 强制使用环境变量

## 🚀 启动方式

### 开发环境
```bash
# 方式1：直接指定profile
java -jar super-agent-boot.jar --spring.profiles.active=dev

# 方式2：使用环境变量
export SPRING_PROFILES_ACTIVE=dev
java -jar super-agent-boot.jar

# 方式3：开发时也可以覆盖数据库配置
DB_USERNAME=dev_user DB_PASSWORD=dev_pass java -jar super-agent-boot.jar --spring.profiles.active=dev
```

### 生产环境
```bash
# 设置必需的环境变量
export DB_URL="jdbc:mysql://prod-db:3306/super_agent?useSSL=true"
export DB_USERNAME="app_user"
export DB_PASSWORD="secure_password_here"
export SERVER_PORT=8080

# 可选环境变量
export API_DOC_ENABLE=false  # 关闭API文档
export DRUID_USERNAME=monitor
export DRUID_PASSWORD=monitor_pass

# 启动应用
java -jar super-agent-boot.jar --spring.profiles.active=prod
```

## 🔧 环境变量一览

### 必需变量（生产环境）
| 变量名 | 描述 | 示例 |
|--------|------|------|
| `DB_PASSWORD` | 数据库密码 | `secure_password` |

### 可选变量
| 变量名 | 默认值 | 描述 |
|--------|--------|------|
| `SERVER_PORT` | `8080` | 服务端口 |
| `DB_URL` | `jdbc:mysql://prod-db-host:3306/...` | 数据库连接 |
| `DB_USERNAME` | `app_user` | 数据库用户名 |
| `API_DOC_ENABLE` | `false` | 是否启用API文档 |
| `DRUID_USERNAME` | `admin` | Druid监控用户名 |
| `DRUID_PASSWORD` | `admin` | Druid监控密码 |

## 🔍 访问地址

### 开发环境
- **应用**: http://localhost:8080
- **API文档**: http://localhost:8080/doc.html
- **Druid监控**: http://localhost:8080/druid (admin/admin)

### 生产环境
- **应用**: http://server:8080
- **API文档**: 默认关闭（可通过环境变量启用）
- **Druid监控**: 已关闭

## 📋 日志说明

### 开发环境日志
- **控制台**: 详细的DEBUG日志
- **文件**: `logs/super-agent-dev.log`
- **SQL打印**: 启用

### 生产环境日志
- **控制台**: 仅WARN及以上级别
- **文件**: `logs/super-agent.log`
- **滚动策略**: 100MB一个文件，保留30天
- **SQL打印**: 禁用

## ⚠️ 安全注意事项

1. **生产环境必须设置** `DB_PASSWORD` 环境变量
2. **不要在配置文件中硬编码**敏感信息
3. **生产环境建议关闭**API文档和监控页面
4. **使用HTTPS**访问生产环境应用
