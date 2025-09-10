#!/bin/bash
#
# Super Agent 启动、停止、重启、状态查询、日志查看脚本
#
# @author Super Agent Team
# @since 1.0.0
#
# 使用方法:
#   ./startup.sh [start|stop|restart|status|logs|debug|daemon]
#
# ==================================================================================================

# === 基本配置 ===
# 应用名称 (必须与 Spring Boot application.name 一致)
APP_NAME="super-agent"
# 主启动类 (必须是完整的类名)
MAIN_CLASS="com.noah.superagent.SuperAgentApplication"

# === 路径和文件配置 ===
# 获取脚本所在目录
SCRIPT_PATH=$(cd "$(dirname "$0")"; pwd)
# 应用根目录 (bin目录的上级目录)
APP_HOME=$(cd "$SCRIPT_PATH/.."; pwd)
# 解析简单参数（第二参数开启URL打印）
CMD="$1"
EXTRA="$2"
if [ "$EXTRA" = "urls" ] || [ "$EXTRA" = "--urls" ]; then
    export URLS=on
fi
# 配置文件目录
CONF_DIR="$APP_HOME/conf"
# 依赖包目录
LIB_DIR="$APP_HOME/lib"
# 业务模块目录
MODULES_DIR="$APP_HOME/modules"
# PID文件路径
PID_FILE="$APP_HOME/bin/${APP_NAME}.pid"
# 日志文件 (由logback管理，这里只用于状态和tail命令)
LOG_FILE="$APP_HOME/logs/${APP_NAME}-info.log"
ERROR_LOG_FILE="$APP_HOME/logs/${APP_NAME}-error.log"

# === Java 和 JVM 配置 ===
# Java命令路径 (如果不在PATH中，请指定绝对路径)
JAVA_CMD="java"
# JVM参数
# -Xms, -Xmx: 初始和最大堆大小
# -Xmn: 新生代大小 (建议为堆大小的1/4到1/3)
# -XX:+UseG1GC: 使用G1垃圾收集器 (适用于大堆内存)
# -XX:+HeapDumpOnOutOfMemoryError: OOM时自动生成堆转储文件
# -Djava.awt.headless=true: 无头模式，适用于服务器环境
# -Dfile.encoding=UTF-8: 文件编码
JVM_OPTS="-server -Xms512m -Xmx1024m -Xmn256m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8"
# 调试模式参数
DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# === Spring Boot 配置 ===
# 指定配置文件位置
SPRING_OPTS="--spring.config.location=$CONF_DIR/"


# ==================================================================================================
#   函数定义
# ==================================================================================================

# 打印配置加载摘要
print_config_summary() {
    echo ""
    echo "🔧 加载的配置文件预览:"
    
    local main_config_file="${CONF_DIR}/application.yml"
    
    if [ -r "$main_config_file" ]; then
        echo "   - 主文件: $main_config_file"
        # 使用awk解析import块，更健壮
        awk '/import:/,/^[^[:space:]]/{if(/classpath:/) {gsub("classpath:",""); printf "     -> %s\n", $2}}' "$main_config_file"
    else
        echo "   - 警告: 主配置文件 application.yml 不存在或不可读。"
    fi
    echo ""
}

# 检查Java环境
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

# 准备启动环境：构建CLASSPATH、创建日志目录、开启ANSI颜色
prepare_startup() {
    mkdir -p "$APP_HOME/logs"
    export SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
    # 若未显式设置，则默认关闭组件URL打印
    if [ -z "$URLS" ]; then
        export URLS=off
    fi
    # 构建classpath，包含conf、lib与modules下的所有jar
    CLASSPATH="$CONF_DIR"
    for jar in "$LIB_DIR"/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
    for jar in "$MODULES_DIR"/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
    for jar in "$MODULES_DIR"/*.jar.original; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
}

# 启动函数
start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "❌ $APP_NAME 正在运行 (PID: $PID)，请先停止。"
            exit 1
        fi
    fi

    # 检查Java环境
    check_java
    
    # 准备启动环境
    prepare_startup
    
    # 打印配置摘要
    print_config_summary

    # 让日志框架(logback)来处理文件写入和控制台输出
    # 不再使用 tee 重定向
    START_CMD="$JAVA_CMD $JVM_OPTS -cp $CLASSPATH $MAIN_CLASS $SPRING_OPTS"

    echo ""
    echo "🚀 启动中，日志将由Logback管理..."
    echo "   - 控制台将显示彩色日志"
    echo "   - 文件日志将写入: $LOG_FILE"
    echo "   - 错误日志将写入: $ERROR_LOG_FILE"
    echo "📋 按 Ctrl+C 可停止应用"
    echo "--------------------------------------------------------------------------------"
    
    # 直接执行，让logback同时输出到控制台和文件
    $START_CMD
}

# 停止函数
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

# 状态检查函数
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

# 日志查看函数
logs() {
    if [ ! -f "$LOG_FILE" ]; then
        echo "❌ 日志文件不存在: $LOG_FILE"
        exit 1
    fi
    
    echo "📋 正在查看日志 (按 Ctrl+C 退出): $LOG_FILE"
    echo "--------------------------------------------------------------------------------"
    tail -f "$LOG_FILE"
}


# 远程调试模式
debug() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "❌ $APP_NAME 正在运行 (PID: $PID)，请先停止再以调试模式启动。"
            exit 1
        fi
    fi

    check_java
    prepare_startup
    print_config_summary

    # 调试端口
    DEBUG_PORT=${3:-5005}
    
    echo "🐛 远程调试模式启动 $APP_NAME ..."
    echo "🔌 调试端口: $DEBUG_PORT"
    echo "🔧 IDE连接: localhost:$DEBUG_PORT"
    echo "📋 日志将同时显示在控制台，文件由Logback管理: $LOG_FILE"

    # 添加调试参数
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"

    echo ""
    echo "🚀 启动中..."

    # 直接执行（不使用tee），日志由Logback接管
    $JAVA_CMD $JVM_OPTS $DEBUG_OPTS -cp "$CLASSPATH" $MAIN_CLASS $SPRING_OPTS
}

# 后台启动函数
daemon() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "❌ $APP_NAME 正在运行 (PID: $PID)，请勿重复启动。"
            exit 1
        fi
    fi

    check_java
    prepare_startup
    print_config_summary

    # 将标准输出和错误重定向到/dev/null，完全交由logback管理日志文件
    START_CMD="nohup $JAVA_CMD $JVM_OPTS -cp $CLASSPATH $MAIN_CLASS $SPRING_OPTS > /dev/null 2>&1 &"

    echo "🚀 后台启动中..."
    eval $START_CMD
    PID=$!
    echo $PID > "$PID_FILE"
    
    # 等待启动
    sleep 3
    if kill -0 "$PID" 2>/dev/null; then
        echo "✅ $APP_NAME 后台启动成功，PID: $PID"
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
        echo "用法: $0 {start|daemon|stop|restart|status|logs|debug} [urls] [debug_port]"
        echo ""
        echo "命令说明:"
        echo "  start   - 前台启动应用（显示日志，Ctrl+C停止）"
        echo "  daemon  - 后台启动应用（守护进程模式）"
        echo "  stop    - 停止应用"
        echo "  restart - 重启应用"
        echo "  status  - 查看运行状态"
        echo "  logs    - 实时查看日志"
        echo "  debug   - 远程调试模式（前台运行，开启JVM调试端口，默认5005）"
        echo ""
        echo "可选参数:"
        echo "  urls    - 启动时打印 Docs/OpenAPI/Actuator/Druid 访问地址"
        echo ""
        echo "示例:"
        echo "  $0 start urls       # 前台启动并打印组件URL"
        echo "  $0 daemon urls      # 后台启动并打印组件URL"
        echo "  $0 debug urls 5005  # 调试模式并打印组件URL"
        exit 1
        ;;
esac

exit $?
