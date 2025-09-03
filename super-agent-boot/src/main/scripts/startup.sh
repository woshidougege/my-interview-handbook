#!/bin/bash

# Super Agent Platform 启动脚本
# 使用方法: ./startup.sh [start|stop|restart|status] [profile]
# 示例: ./startup.sh start dev

APP_NAME="super-agent-boot"
APP_JAR="lib/${APP_NAME}.jar"
PID_FILE="logs/${APP_NAME}.pid"
LOG_FILE="logs/${APP_NAME}.log"

# 获取脚本所在目录
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
cd "$APP_HOME"

# 获取profile参数
PROFILE=${2:-dev}

# JVM参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/"
SPRING_OPTS="--spring.config.location=conf/ --spring.profiles.active=${PROFILE} --logging.file.path=logs/"

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
    mkdir -p logs
    
    nohup $JAVA_CMD $JVM_OPTS -jar "$APP_JAR" $SPRING_OPTS > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"
    
    # 等待启动
    sleep 3
    if kill -0 "$PID" 2>/dev/null; then
        echo "✅ $APP_NAME 启动成功，PID: $PID"
        echo "📖 API文档: http://localhost:8081/doc.html"
        echo "📊 监控页面: http://localhost:8081/druid"
        echo "📝 日志文件: $LOG_FILE"
    else
        echo "❌ $APP_NAME 启动失败，请检查日志: $LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
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

# 重启应用
restart() {
    echo "🔄 重启 $APP_NAME..."
    stop
    sleep 2
    start
}

# 主入口
case "$1" in
    start)
        start
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
    *)
        echo "用法: $0 {start|stop|restart|status} [profile]"
        echo ""
        echo "命令说明:"
        echo "  start   - 启动应用"
        echo "  stop    - 停止应用"
        echo "  restart - 重启应用"
        echo "  status  - 查看运行状态"
        echo ""
        echo "Profile说明:"
        echo "  dev     - 开发环境（默认）"
        echo "  test    - 测试环境"
        echo "  prod    - 生产环境"
        echo ""
        echo "示例:"
        echo "  $0 start dev    # 以开发环境启动"
        echo "  $0 start prod   # 以生产环境启动"
        exit 1
        ;;
esac

exit $?
