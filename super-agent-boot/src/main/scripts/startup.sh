#!/bin/bash

# Super Agent Platform 启动脚本
# 使用方法: ./startup.sh [start|stop|restart|status] [profile]
# 示例: ./startup.sh start dev

APP_NAME="super-agent"
MAIN_JAR="modules/super-agent-boot.jar.original"
PID_FILE="logs/${APP_NAME}.pid"
LOG_FILE="logs/${APP_NAME}.log"

# 获取脚本所在目录
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
cd "$APP_HOME"

# 获取profile参数
PROFILE=${2:-dev}

# JVM参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/"
SPRING_OPTS="--spring.config.location=conf/ --spring.profiles.active=${PROFILE} --logging.file.path=logs/ --spring.flyway.locations=classpath:conf/db/migration"

# 检查Java环境
if [ -z "$JAVA_HOME" ]; then
    JAVA_CMD="java"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

# 检查Java版本
check_java() {
    if ! command -v $JAVA_CMD &> /dev/null; then
        echo "❌ 错误: 未找到Java运行环境，请安装JDK 11+"
        exit 1
    fi
    
    JAVA_VERSION=$($JAVA_CMD -version 2>&1 | grep "version" | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt "11" ]; then
        echo "❌ 错误: Java版本过低，需要JDK 11+，当前版本: $JAVA_VERSION"
        exit 1
    fi
}

# 启动应用
start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo "⚠️  应用已在运行中，PID: $PID"
            return 1
        else
            echo "🔄 清理无效的PID文件"
            rm -f "$PID_FILE"
        fi
    fi

    check_java
    
    echo "🚀 启动 $APP_NAME (Profile: $PROFILE)..."
    echo "📋 工作目录: $(pwd)"
    mkdir -p logs
    
    # 强制启用颜色输出
    export SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
    
    # 检查主启动jar是否存在
    if [ ! -f "$MAIN_JAR" ]; then
        echo "❌ 错误: 找不到主启动jar文件: $MAIN_JAR"
        echo "📋 请检查打包是否正确"
        ls -la modules/ 2>/dev/null || echo "modules目录不存在"
        return 1
    fi
    
    echo "📦 使用分离依赖启动方式"
    
    # 构建classpath，包含配置目录、lib和modules目录下的所有jar
    CLASSPATH="conf"
    echo "📦 构建Classpath..."
    
    # 添加第三方依赖
    LIB_COUNT=0
    for jar in lib/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
            LIB_COUNT=$((LIB_COUNT + 1))
        fi
    done
    echo "  找到 $LIB_COUNT 个第三方依赖jar"
    
    # 添加业务模块jar
    MODULE_COUNT=0
    for jar in modules/*.jar*; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
            MODULE_COUNT=$((MODULE_COUNT + 1))
            echo "  + $jar"
        fi
    done
    echo "  找到 $MODULE_COUNT 个业务模块jar"
    
    if [ $MODULE_COUNT -eq 0 ]; then
        echo "❌ 错误: 在modules目录下找不到任何jar文件！"
        echo "📋 请检查打包是否正确"
        ls -la modules/ 2>/dev/null || echo "modules目录不存在"
        return 1
    fi
    
    # 检查主类是否存在
    echo "🔍 检查主类是否存在..."
    if jar tf "$MAIN_JAR" | grep -q "com/noah/superagent/SuperAgentApplication.class"; then
        echo "✅ 找到主类: com.noah.superagent.SuperAgentApplication"
    else
        echo "❌ 错误: 在$MAIN_JAR中找不到主类！"
        echo "📋 jar包内容（前20行）："
        jar tf "$MAIN_JAR" | head -20
        echo "..."
        echo "📋 jar包主类配置："
        jar xf "$MAIN_JAR" META-INF/MANIFEST.MF -O 2>/dev/null | grep -i "main-class" || echo "未找到主类配置"
        return 1
    fi
    
    START_CMD="$JAVA_CMD $JVM_OPTS -cp $CLASSPATH com.noah.superagent.SuperAgentApplication $SPRING_OPTS"
    
    echo ""
    echo "🔧 JVM参数: $JVM_OPTS"
    echo "🔧 Spring参数: $SPRING_OPTS"
    echo "🔧 启动命令: $START_CMD"
    echo ""
    echo "🚀 启动中，日志将持续显示在控制台..."
    echo "📋 按 Ctrl+C 可停止应用"
    echo "📖 API文档: http://localhost:8081/super-agent/swagger-ui.html"
    echo "📋 API定义: http://localhost:8081/super-agent/v3/api-docs"
    echo "📊 监控页面: http://localhost:8081/super-agent/druid"
    echo ""
    
    # 前台运行，同时写入日志文件，直到用户按Ctrl+C
    $START_CMD 2>&1 | tee "$LOG_FILE"
}

# 停止应用
stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "⚠️  应用未运行"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    if ! kill -0 "$PID" 2>/dev/null; then
        echo "⚠️  应用未运行，清理PID文件"
        rm -f "$PID_FILE"
        return 1
    fi
    
    echo "🛑 停止 $APP_NAME (PID: $PID)..."
    kill "$PID"
    
    # 等待进程结束
    for i in {1..30}; do
        if ! kill -0 "$PID" 2>/dev/null; then
            echo "✅ $APP_NAME 已停止"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done
    
    # 强制杀死
    echo "⚠️  强制停止 $APP_NAME..."
    kill -9 "$PID" 2>/dev/null
    rm -f "$PID_FILE"
    echo "✅ $APP_NAME 已强制停止"
}

# 查看状态
status() {
    if [ ! -f "$PID_FILE" ]; then
        echo "📋 状态: $APP_NAME 未运行"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "📋 状态: $APP_NAME 正在运行，PID: $PID"
        echo "💾 内存使用: $(ps -o pid,ppid,rss,comm -p $PID | tail -1 | awk '{print $3/1024 "MB"}')"
        return 0
    else
        echo "📋 状态: $APP_NAME 未运行（PID文件存在但进程不存在）"
        rm -f "$PID_FILE"
        return 1
    fi
}

# 重启应用（后台模式）
restart() {
    echo "🔄 重启 $APP_NAME..."
    stop
    sleep 2
    daemon
}

# 查看日志
logs() {
    if [ ! -f "$LOG_FILE" ]; then
        echo "📝 日志文件不存在: $LOG_FILE"
        return 1
    fi
    
    if [ "$2" = "tail" ] || [ "$2" = "f" ]; then
        echo "📝 实时查看日志 (Ctrl+C 退出):"
        tail -f "$LOG_FILE"
    else
        echo "📝 显示最近100行日志:"
        tail -100 "$LOG_FILE"
    fi
}


# 远程调试模式
debug() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo "⚠️  应用已在运行中，PID: $PID，请先停止"
            return 1
        else
            echo "🔄 清理无效的PID文件"
            rm -f "$PID_FILE"
        fi
    fi

    check_java
    
    # 调试端口
    DEBUG_PORT=${3:-5005}
    
    echo "🐛 远程调试模式启动 $APP_NAME (Profile: $PROFILE)..."
    echo "🔌 调试端口: $DEBUG_PORT"
    echo "🔧 IDE连接: localhost:$DEBUG_PORT"
    echo "📋 日志将同时显示在控制台和写入文件: $LOG_FILE"
    echo "📋 按 Ctrl+C 停止应用"
    mkdir -p logs
    
    # 构建classpath
    CLASSPATH="conf"
    for jar in lib/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
    for jar in modules/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
    
    # 添加调试参数
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
    
    echo ""
    echo "🚀 启动中..."
    
    # 使用tee同时输出到控制台和文件
    $JAVA_CMD $JVM_OPTS $DEBUG_OPTS -cp "$CLASSPATH" com.noah.superagent.SuperAgentApplication $SPRING_OPTS 2>&1 | tee "$LOG_FILE"
}

# 后台启动
daemon() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo "⚠️  应用已在运行中，PID: $PID"
            return 1
        else
            echo "🔄 清理无效的PID文件"
            rm -f "$PID_FILE"
        fi
    fi

    check_java
    
    echo "🚀 后台启动 $APP_NAME (Profile: $PROFILE)..."
    echo "📋 工作目录: $(pwd)"
    mkdir -p logs
    
    # 强制启用颜色输出
    export SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
    
    # 检查主启动jar是否存在
    if [ ! -f "$MAIN_JAR" ]; then
        echo "❌ 错误: 找不到主启动jar文件: $MAIN_JAR"
        echo "📋 请检查打包是否正确"
        ls -la modules/ 2>/dev/null || echo "modules目录不存在"
        return 1
    fi
    
    echo "📦 使用分离依赖启动方式"
    
    # 构建classpath
    CLASSPATH="conf"
    
    # 添加第三方依赖
    LIB_COUNT=0
    for jar in lib/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
            LIB_COUNT=$((LIB_COUNT + 1))
        fi
    done
    echo "  找到 $LIB_COUNT 个第三方依赖jar"
    
    # 添加业务模块jar
    MODULE_COUNT=0
    for jar in modules/*.jar*; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
            MODULE_COUNT=$((MODULE_COUNT + 1))
        fi
    done
    echo "  找到 $MODULE_COUNT 个业务模块jar"
    
    START_CMD="$JAVA_CMD $JVM_OPTS -cp $CLASSPATH com.noah.superagent.SuperAgentApplication $SPRING_OPTS"
    
    echo ""
    echo "🔧 启动命令: $START_CMD"
    echo ""
    echo "🚀 后台启动中..."
    
    # 后台启动
    nohup $START_CMD > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"
    
    # 等待启动
    sleep 3
    if kill -0 "$PID" 2>/dev/null; then
        echo "✅ $APP_NAME 后台启动成功，PID: $PID"
        echo "📖 API文档: http://localhost:8081/super-agent/swagger-ui.html"
        echo "📋 API定义: http://localhost:8081/super-agent/v3/api-docs"
        echo "📊 监控页面: http://localhost:8081/super-agent/druid"
        echo "📝 日志文件: $LOG_FILE"
        echo "📝 查看实时日志: $0 logs tail"
    else
        echo "❌ $APP_NAME 启动失败，请检查日志: $LOG_FILE"
        echo "📝 显示最新错误日志:"
        tail -20 "$LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
}

# 主入口
case "$1" in
    start)
        start
        ;;
    daemon)
        daemon
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs "$@"
        ;;
    debug)
        debug
        ;;
    *)
        echo "用法: $0 {start|daemon|stop|restart|status|logs|debug} [profile] [debug_port]"
        echo ""
        echo "命令说明:"
        echo "  start   - 前台启动应用（显示日志，Ctrl+C停止）"
        echo "  daemon  - 后台启动应用（守护进程模式）"
        echo "  stop    - 停止应用"
        echo "  restart - 重启应用"
        echo "  status  - 查看运行状态"
        echo "  logs    - 查看日志（最近100行）"
        echo "  logs tail - 实时查看日志"
        echo "  debug   - 远程调试模式（前台运行，开启JVM调试端口）"
        echo ""
        echo "Profile说明:"
        echo "  dev     - 开发环境（默认）"
        echo "  test    - 测试环境"
        echo "  prod    - 生产环境"
        echo ""
        echo "示例:"
        echo "  $0 start dev          # 前台启动开发环境"
        echo "  $0 daemon prod        # 后台启动生产环境"
        echo "  $0 debug dev 5005     # 开启远程调试，端口5005"
        echo "  $0 logs tail          # 实时查看日志"
        exit 1
        ;;
esac

exit $?
