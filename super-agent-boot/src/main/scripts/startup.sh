#!/bin/bash
#
# Super Agent 启动、停止、重启、状态查询、日志查看脚本
#
# @author 任相鹏
# @since 1.0.0
#
# 使用方法:
#   ./startup.sh [start|stop|restart|status|logs|debug|daemon|cleanup]
#
# 项目目录结构:
#   ├── bin/           # 启动脚本目录
#   ├── conf/          # 配置文件目录
#   ├── lib/           # 依赖库目录
#   ├── modules/       # 业务模块目录
#   ├── logs/          # 日志文件目录
#   └── pid/           # PID文件目录
#
# ==================================================================================================

# === 颜色定义 ===
# ANSI 颜色代码
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
BLACK='\033[0;30m'
BOLD='\033[1m'
UNDERLINE='\033[4m'
# 背景色
BG_RED='\033[41m'
BG_GREEN='\033[42m'
BG_YELLOW='\033[43m'
BG_BLUE='\033[44m'
BG_PURPLE='\033[45m'
BG_CYAN='\033[46m'
# 重置颜色
NC='\033[0m' # No Color

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
# 注意：现在使用Spring Boot内置PID管理（推荐方式）
# Spring Boot会自动创建和管理PID文件，确保与应用生命周期同步
# 使用独立的pid目录，与bin、logs、conf等目录同级
PID_FILE="$APP_HOME/pid/${APP_NAME}.pid"
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
    echo -e "${CYAN}🔧 加载的配置文件预览:${NC}"
    
    local main_config_file="${CONF_DIR}/application.yml"
    
    if [ -r "$main_config_file" ]; then
        echo -e "   ${GREEN}- 主文件:${NC} ${BLUE}$main_config_file${NC}"
        # 使用awk解析import块，更健壮
        awk '/import:/,/^[^[:space:]]/{if(/classpath:/) {gsub("classpath:",""); printf "     \033[0;35m-> \033[0;33m%s\033[0m\n", $2}}' "$main_config_file"
    else
        echo -e "   ${YELLOW}- 警告:${NC} ${RED}主配置文件 application.yml 不存在或不可读。${NC}"
    fi
    echo ""
}

# 检查Java环境
check_java() {
    if ! command -v $JAVA_CMD &> /dev/null; then
        echo -e "${RED}❌ 错误:${NC} 未找到Java运行环境，请安装JDK 11+"
        exit 1
    fi
    
    JAVA_VERSION=$($JAVA_CMD -version 2>&1 | grep "version" | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt "11" ]; then
        echo -e "${RED}❌ 错误:${NC} Java版本过低，需要JDK 11+，当前版本: ${YELLOW}$JAVA_VERSION${NC}"
        exit 1
    fi
}

# 准备启动环境：构建CLASSPATH、创建必要目录、开启ANSI颜色
prepare_startup() {
    # 创建必要的目录
    mkdir -p "$APP_HOME/logs"
    mkdir -p "$APP_HOME/pid"
    export SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
    # 设置日志路径为绝对路径，确保无论在哪个目录启动都写入到项目根目录
    export LOG_PATH="$APP_HOME/logs"
    # 设置PID文件路径，让Spring Boot自己管理PID文件
    export PID_FILE_PATH="$PID_FILE"
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

# 清理旧的提示进程
cleanup_old_prompts() {
    local prompt_pid_file="/tmp/${APP_NAME}_prompt.pid"
    
    # 方法1：清理PID文件中记录的进程
    if [ -f "$prompt_pid_file" ]; then
        local old_pid=$(cat "$prompt_pid_file" 2>/dev/null)
        if [ -n "$old_pid" ]; then
            # 尝试杀死进程组
            kill -TERM -"$old_pid" 2>/dev/null
            sleep 0.5
            kill -KILL -"$old_pid" 2>/dev/null
            # 单独杀死主进程
            kill "$old_pid" 2>/dev/null
            kill -9 "$old_pid" 2>/dev/null
        fi
        rm -f "$prompt_pid_file"
    fi
    
    # 方法2：通过进程命令行特征查找并清理所有相关进程
    # 查找包含sleep 5和printf的后台进程（我们的提示进程特征）
    local pids=$(ps aux | grep -E "(sleep 5|printf.*按 Ctrl\+C)" | grep -v grep | awk '{print $2}' 2>/dev/null)
    if [ -n "$pids" ]; then
        echo "$pids" | xargs kill -9 2>/dev/null || true
    fi
    
    # 方法3：清理所有包含"super-agent"和"prompt"关键字的进程
    pkill -f "${APP_NAME}.*prompt" 2>/dev/null || true
    
    # 方法4：删除标记文件，让使用标记文件的进程自然退出
    rm -f "/tmp/${APP_NAME}_prompt_marker"
    
    # 清除底部提示并重置终端
    printf "\033[999;1H\033[K\033[0m"
}

# 显示持续提示的函数
show_persistent_prompt() {
    local prompt_pid_file="/tmp/${APP_NAME}_prompt.pid"
    local prompt_marker="/tmp/${APP_NAME}_prompt_marker"
    
    # 先清理旧的进程
    cleanup_old_prompts
    
    # 创建提示进程标记文件，便于识别
    echo "super-agent-prompt-process" > "$prompt_marker"
    
    # 使用setsid创建新的进程组，便于管理
    setsid bash -c "
        # 在子进程中也设置标记
        echo \$\$ > '$prompt_pid_file'
        
        # 设置子进程的清理函数
        cleanup_child() {
            printf '\033[999;1H\033[K\033[0m'
            rm -f '$prompt_marker' '$prompt_pid_file'
            exit
        }
        trap cleanup_child INT TERM EXIT QUIT HUP
        
        # 主循环
        while [ -f '$prompt_marker' ]; do
            sleep 5
            # 检查标记文件是否还存在，如果不存在则退出
            if [ ! -f '$prompt_marker' ]; then
                break
            fi
            # 保存当前光标位置，移动到屏幕底部，显示提示，然后恢复光标位置
            printf '\033[s\033[999;1H\033[K\033[44;37m 📋 按 Ctrl+C 可停止应用 \033[0m\033[u'
        done
        
        # 清理并退出
        printf '\033[999;1H\033[K\033[0m'
        rm -f '$prompt_marker' '$prompt_pid_file'
    " &
    
    local prompt_pid=$!
    echo $prompt_pid > "$prompt_pid_file"
    
    # 增强的清理函数
    cleanup_prompt() {
        # 删除标记文件，让子进程自然退出
        rm -f "$prompt_marker"
        
        # 等待一小段时间让子进程自然退出
        sleep 1
        
        # 强制清理
        if [ -f "$prompt_pid_file" ]; then
            local saved_pid=$(cat "$prompt_pid_file" 2>/dev/null)
            if [ -n "$saved_pid" ]; then
                # 杀死整个进程组
                kill -TERM -"$saved_pid" 2>/dev/null
                sleep 0.5
                kill -KILL -"$saved_pid" 2>/dev/null
                # 杀死主进程
                kill "$saved_pid" 2>/dev/null
                kill -9 "$saved_pid" 2>/dev/null
            fi
            rm -f "$prompt_pid_file"
        fi
        
        # 全面清理
        cleanup_old_prompts
        
        # 清除底部提示行并重置终端
        printf "\033[999;1H\033[K\033[0m"
        exit
    }
    
    # 设置多种信号陷阱
    trap cleanup_prompt INT TERM EXIT QUIT HUP PIPE
}

# 启动函数
start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${RED}❌ $APP_NAME 正在运行 (PID: ${YELLOW}$PID${RED})，请先停止。${NC}"
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
    echo -e "${GREEN}🚀 启动中，日志将由Logback管理...${NC}"
    echo -e "   ${CYAN}- 控制台将显示彩色日志${NC}"
    echo -e "   ${CYAN}- 文件日志将写入:${NC} ${BLUE}$LOG_FILE${NC}"
    echo -e "   ${CYAN}- 错误日志将写入:${NC} ${BLUE}$ERROR_LOG_FILE${NC}"
    echo -e "${BG_BLUE}${WHITE} 📋 按 Ctrl+C 可停止应用 ${NC}"
    echo -e "${CYAN}--------------------------------------------------------------------------------${NC}"
    
    # 启动持续提示显示
    show_persistent_prompt
    
    # 直接执行，让logback同时输出到控制台和文件
    $START_CMD
}

# 停止函数
stop() {
    # 先清理可能存在的提示进程
    cleanup_old_prompts
    
    if [ ! -f "$PID_FILE" ]; then
        echo -e "${YELLOW}⚠️  应用未运行${NC}"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    if ! kill -0 "$PID" 2>/dev/null; then
        echo -e "${YELLOW}⚠️  应用未运行，清理PID文件${NC}"
        rm -f "$PID_FILE"
        return 1
    fi
    
    echo -e "${PURPLE}🛑 停止 ${BOLD}$APP_NAME${NC} ${PURPLE}(PID: ${YELLOW}$PID${PURPLE})...${NC}"
    kill "$PID"
    
    # 等待进程结束
    for i in {1..30}; do
        if ! kill -0 "$PID" 2>/dev/null; then
            echo -e "${GREEN}✅ $APP_NAME 已停止${NC}"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done
    
    # 强制杀死
    echo -e "${YELLOW}⚠️  强制停止 $APP_NAME...${NC}"
    kill -9 "$PID" 2>/dev/null
    rm -f "$PID_FILE"
    echo -e "${GREEN}✅ $APP_NAME 已强制停止${NC}"
}

# 状态检查函数
status() {
    if [ ! -f "$PID_FILE" ]; then
        echo -e "${CYAN}📋 状态:${NC} ${RED}$APP_NAME 未运行${NC}"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo -e "${CYAN}📋 状态:${NC} ${GREEN}$APP_NAME 正在运行${NC}，${CYAN}PID:${NC} ${YELLOW}$PID${NC}"
        echo -e "${CYAN}💾 内存使用:${NC} ${PURPLE}$(ps -o pid,ppid,rss,comm -p $PID | tail -1 | awk '{print $3/1024 "MB"}')${NC}"
        return 0
    else
        echo -e "${CYAN}📋 状态:${NC} ${YELLOW}$APP_NAME 未运行（PID文件存在但进程不存在）${NC}"
        rm -f "$PID_FILE"
        return 1
    fi
}

# 重启应用（后台模式）
restart() {
    echo -e "${BLUE}🔄 重启 ${BOLD}$APP_NAME${NC}${BLUE}...${NC}"
    stop
    sleep 2
    daemon
}

# 日志查看函数
logs() {
    if [ ! -f "$LOG_FILE" ]; then
        echo -e "${RED}❌ 日志文件不存在:${NC} ${BLUE}$LOG_FILE${NC}"
        exit 1
    fi
    
    echo -e "${CYAN}📋 正在查看日志 ${YELLOW}(按 Ctrl+C 退出)${CYAN}:${NC} ${BLUE}$LOG_FILE${NC}"
    echo -e "${CYAN}--------------------------------------------------------------------------------${NC}"
    tail -f "$LOG_FILE"
}


# 远程调试模式
debug() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${RED}❌ $APP_NAME 正在运行 (PID: ${YELLOW}$PID${RED})，请先停止再以调试模式启动。${NC}"
            exit 1
        fi
    fi

    check_java
    prepare_startup
    print_config_summary

    # 调试端口
    DEBUG_PORT=${3:-5005}
    
    echo -e "${PURPLE}🐛 远程调试模式启动 ${BOLD}$APP_NAME${NC} ${PURPLE}...${NC}"
    echo -e "${CYAN}🔌 调试端口:${NC} ${YELLOW}$DEBUG_PORT${NC}"
    echo -e "${CYAN}🔧 IDE连接:${NC} ${GREEN}localhost:$DEBUG_PORT${NC}"
    echo -e "${CYAN}📋 日志将同时显示在控制台，文件由Logback管理:${NC} ${BLUE}$LOG_FILE${NC}"
    echo -e "${BG_PURPLE}${WHITE} 📋 按 Ctrl+C 可停止应用 ${NC}"

    # 添加调试参数
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"

    echo ""
    echo -e "${GREEN}🚀 启动中...${NC}"
    echo -e "${CYAN}--------------------------------------------------------------------------------${NC}"

    # 启动持续提示显示
    show_persistent_prompt

    # 直接执行（不使用tee），日志由Logback接管
    $JAVA_CMD $JVM_OPTS $DEBUG_OPTS -cp "$CLASSPATH" $MAIN_CLASS $SPRING_OPTS
}

# 后台启动函数
daemon() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${RED}❌ $APP_NAME 正在运行 (PID: ${YELLOW}$PID${RED})，请勿重复启动。${NC}"
            exit 1
        fi
    fi

    check_java
    prepare_startup
    print_config_summary

    echo -e "${GREEN}🚀 后台启动中...${NC}"
    echo -e "${CYAN}📋 使用Spring Boot内置PID管理${NC}"
    echo -e "${CYAN}📄 PID文件:${NC} ${BLUE}$PID_FILE${NC}"
    
    # 让Spring Boot自己管理PID文件，不再手动写入
    nohup $JAVA_CMD $JVM_OPTS -cp "$CLASSPATH" $MAIN_CLASS $SPRING_OPTS > /dev/null 2>&1 &
    local java_pid=$!
    
    # 等待Spring Boot创建PID文件和应用启动
    echo -e "${YELLOW}⏳ 等待应用启动和PID文件生成...${NC}"
    for i in {1..30}; do
        # 检查Java进程是否还在运行
        if ! kill -0 "$java_pid" 2>/dev/null; then
            echo -e "${RED}❌ Java进程已退出，启动失败${NC}"
            break
        fi
        
        # 检查PID文件是否生成
        if [ -f "$PID_FILE" ]; then
            local app_pid=$(cat "$PID_FILE" 2>/dev/null)
            if [ -n "$app_pid" ] && [ "$app_pid" -eq "$java_pid" ] 2>/dev/null; then
                echo -e "${GREEN}✅ $APP_NAME v1.0.0 启动成功！${NC}"
                echo -e "${CYAN}📄 PID:${NC} ${YELLOW}$app_pid${NC} ${CYAN}(文件:${NC} ${BLUE}$PID_FILE${CYAN})${NC}"
                echo -e "${CYAN}📝 日志目录:${NC} ${BLUE}$LOG_FILE${NC}"
                echo -e "${CYAN}📝 查看实时日志:${NC} ${GREEN}$0 logs${NC}"
                return 0
            fi
        fi
        
        # 显示进度
        if [ $((i % 5)) -eq 0 ]; then
            echo -e "   ${CYAN}... 仍在等待 (${i}s)${NC}"
        fi
        sleep 1
    done
    
    # 启动失败处理
    echo -e "${RED}❌ $APP_NAME 启动失败或超时${NC}"
    if kill -0 "$java_pid" 2>/dev/null; then
        echo -e "${YELLOW}🛑 清理Java进程:${NC} ${PURPLE}$java_pid${NC}"
        kill "$java_pid" 2>/dev/null
    fi
    echo -e "${CYAN}📝 显示最新错误日志:${NC}"
    tail -20 "$LOG_FILE" 2>/dev/null || echo -e "${RED}日志文件不存在${NC}"
    return 1
}

# 显示菜单选项
display_menu() {
    local selected=$1
    local start_line=$2
    
    # 保存光标位置
    echo -ne "\033[s"
    
    # 移动到菜单开始位置
    echo -ne "\033[${start_line};1H"
    
    local options=(
        "🚀 前台启动 (start)"
        "🌙 后台启动 (daemon)" 
        "🛑 停止应用 (stop)"
        "🔄 重启应用 (restart)"
        "📋 查看状态 (status)"
        "📝 查看日志 (logs)"
        "🐛 调试模式 (debug)"
        "🧹 清理进程 (cleanup)"
        "🌐 前台启动+URL显示"
        "🌙 后台启动+URL显示"
        "❌ 退出"
    )
    
    local i=0
    for option in "${options[@]}"; do
        i=$((i + 1))
        # 清除当前行
        echo -ne "\033[2K"
        
        if [ $i -eq $selected ]; then
            # 高亮显示选中项 - 使用白色字体确保在任何背景下都清晰
            echo -e "  ${BG_CYAN}${WHITE}${BOLD} ► $i) $option ${NC}"
        else
            # 普通显示
            echo -e "  ${CYAN}$i)${NC} $option"
        fi
    done
    
    # 恢复光标位置
    echo -ne "\033[u"
}

# 执行选中的菜单项
execute_menu_option() {
    local choice=$1
    
    case $choice in
        1)
            echo -e "${GREEN}正在执行: 前台启动...${NC}"
            start
            return 1  # 退出菜单
            ;;
        2)
            echo -e "${GREEN}正在执行: 后台启动...${NC}"
            daemon
            return 1  # 退出菜单
            ;;
        3)
            echo -e "${GREEN}正在执行: 停止应用...${NC}"
            stop
            echo ""
            echo -e "${CYAN}按任意键继续...${NC}"
            read -n 1 -s
            return 0  # 返回菜单
            ;;
        4)
            echo -e "${GREEN}正在执行: 重启应用...${NC}"
            restart
            return 1  # 退出菜单
            ;;
        5)
            echo -e "${GREEN}正在执行: 查看状态...${NC}"
            status
            echo ""
            echo -e "${CYAN}按任意键继续...${NC}"
            read -n 1 -s
            return 0  # 返回菜单
            ;;
        6)
            echo -e "${GREEN}正在执行: 查看日志...${NC}"
            logs
            return 1  # 退出菜单
            ;;
        7)
            echo -e "${GREEN}正在执行: 调试模式...${NC}"
            debug
            return 1  # 退出菜单
            ;;
        8)
            echo -e "${GREEN}正在执行: 清理进程...${NC}"
            cleanup_old_prompts
            echo -e "${GREEN}✅ 已清理所有提示进程${NC}"
            echo ""
            echo -e "${CYAN}按任意键继续...${NC}"
            read -n 1 -s
            return 0  # 返回菜单
            ;;
        9)
            echo -e "${GREEN}正在执行: 前台启动+URL显示...${NC}"
            export URLS=on
            start
            return 1  # 退出菜单
            ;;
        10)
            echo -e "${GREEN}正在执行: 后台启动+URL显示...${NC}"
            export URLS=on
            daemon
            return 1  # 退出菜单
            ;;
        11)
            echo -e "${PURPLE}👋 再见！${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}❌ 无效选项${NC}"
            sleep 1
            return 0  # 返回菜单
            ;;
    esac
}

# 交互式菜单函数
interactive_menu() {
    # 设置终端为原始模式，捕获特殊键
    local old_stty=$(stty -g)
    stty -echo -icanon min 0 time 10
    
    # 清理函数
    cleanup_terminal() {
        stty "$old_stty"
        echo -ne "\033[?25h"  # 显示光标
    }
    
    # 设置退出陷阱
    trap cleanup_terminal EXIT INT TERM
    
    local selected=1
    local max_options=11
    local menu_start_line
    
    while true; do
        clear
        echo -e "${BOLD}${BG_BLUE}${WHITE}                                                               ${NC}"
        echo -e "${BOLD}${BG_BLUE}${WHITE}    🚀 Super Agent 交互式管理菜单                           ${NC}"
        echo -e "${BOLD}${BG_BLUE}${WHITE}                                                               ${NC}"
        echo ""
        
        # 显示当前状态
        echo -e "${CYAN}📊 当前状态:${NC}"
        if [ -f "$PID_FILE" ]; then
            local pid=$(cat "$PID_FILE" 2>/dev/null)
            if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                echo -e "   ${GREEN}✅ $APP_NAME 正在运行 (PID: ${YELLOW}$pid${GREEN})${NC}"
            else
                echo -e "   ${RED}❌ $APP_NAME 未运行 (PID文件存在但进程不存在)${NC}"
            fi
        else
            echo -e "   ${RED}❌ $APP_NAME 未运行${NC}"
        fi
        echo ""
        
        # 操作提示
        echo -e "${BOLD}${CYAN}请选择操作:${NC}"
        echo -e "${YELLOW}💡 使用 ↑↓ 方向键选择，Enter 确认，数字键直接选择，ESC/q 退出${NC}"
        echo ""
        
        # 记录菜单开始行号
        menu_start_line=$(tput cuf 0; echo $(($(tput cuf 0; tput cuu 100; tput cud 100) + $(echo | wc -l) + 10)))
        menu_start_line=12  # 简化：直接设置固定行号
        
        # 隐藏光标
        echo -ne "\033[?25l"
        
        # 显示菜单
        display_menu "$selected" "$menu_start_line"
        
        # 移动到提示位置
        echo -ne "\033[25;1H"
        echo -ne "\033[2K"
        echo -ne "${YELLOW}👉 当前选择: ${GREEN}选项 $selected${NC} | ${CYAN}操作: ↑↓选择 Enter确认 ESC退出${NC}"
        
        # 读取用户输入
        local key
        read -n 1 key 2>/dev/null
        
        case "$key" in
            $'\033')  # ESC序列开始或ESC键
                read -n 1 -t 0.1 key2 2>/dev/null
                if [ -z "$key2" ]; then
                    # 单独的ESC键，退出
                    cleanup_terminal
                    echo -e "${PURPLE}👋 再见！${NC}"
                    exit 0
                elif [ "$key2" = "[" ]; then
                    # 方向键序列
                    read -n 1 key3 2>/dev/null
                    case "$key3" in
                        'A')  # 上箭头
                            selected=$((selected - 1))
                            if [ $selected -lt 1 ]; then
                                selected=$max_options
                            fi
                            ;;
                        'B')  # 下箭头
                            selected=$((selected + 1))
                            if [ $selected -gt $max_options ]; then
                                selected=1
                            fi
                            ;;
                    esac
                fi
                ;;
            '')  # Enter键
                cleanup_terminal
                clear
                if ! execute_menu_option "$selected"; then
                    # 重新设置终端模式继续菜单
                    stty -echo -icanon min 0 time 10
                    continue
                else
                    break
                fi
                ;;
            'q'|'Q')  # q键退出
                cleanup_terminal
                echo -e "${PURPLE}👋 再见！${NC}"
                exit 0
                ;;
            '1')  # 数字1，可能是10或11
                read -n 1 -t 0.2 key2 2>/dev/null
                local choice_num="1"
                if [ "$key2" = "0" ]; then
                    choice_num="10"
                elif [ "$key2" = "1" ]; then
                    choice_num="11"
                fi
                
                if [ "$choice_num" -le "$max_options" ] && [ "$choice_num" -ge "1" ]; then
                    cleanup_terminal
                    clear
                    if ! execute_menu_option "$choice_num"; then
                        stty -echo -icanon min 0 time 10
                        continue
                    else
                        break
                    fi
                fi
                ;;
            [2-9])  # 数字键2-9
                if [ "$key" -le "$max_options" ] && [ "$key" -ge "1" ]; then
                    cleanup_terminal
                    clear
                    if ! execute_menu_option "$key"; then
                        stty -echo -icanon min 0 time 10
                        continue
                    else
                        break
                    fi
                fi
                ;;
        esac
    done
    
    cleanup_terminal
}

# 主入口
case "$1" in
    -i|--interactive|interactive)
        interactive_menu
        ;;
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
    cleanup)
        cleanup_old_prompts
        echo -e "${GREEN}✅ 已清理所有提示进程${NC}"
        ;;
    *)
        echo -e "${BOLD}${BLUE}用法:${NC} ${GREEN}$0${NC} ${YELLOW}{start|daemon|stop|restart|status|logs|debug|cleanup|-i}${NC} ${CYAN}[urls] [debug_port]${NC}"
        echo ""
        echo -e "${BOLD}${CYAN}命令说明:${NC}"
        echo -e "  ${GREEN}start${NC}        - 前台启动应用（显示日志，Ctrl+C停止）"
        echo -e "  ${GREEN}daemon${NC}       - 后台启动应用（守护进程模式）"
        echo -e "  ${GREEN}stop${NC}         - 停止应用"
        echo -e "  ${GREEN}restart${NC}      - 重启应用"
        echo -e "  ${GREEN}status${NC}       - 查看运行状态"
        echo -e "  ${GREEN}logs${NC}         - 实时查看日志"
        echo -e "  ${GREEN}debug${NC}        - 远程调试模式（前台运行，开启JVM调试端口，默认5005）"
        echo -e "  ${GREEN}cleanup${NC}      - 清理残留的提示进程"
        echo -e "  ${GREEN}-i${NC}/${GREEN}interactive${NC} - 📱 ${BOLD}交互式菜单模式${NC} ${YELLOW}(推荐)${NC}"
        echo ""
        echo -e "${BOLD}${CYAN}可选参数:${NC}"
        echo -e "  ${YELLOW}urls${NC}         - 启动时打印 Docs/OpenAPI/Actuator/Druid 访问地址"
        echo ""
        echo -e "${BOLD}${CYAN}示例:${NC}"
        echo -e "  ${GREEN}$0 -i${NC}               ${PURPLE}# 🎯 交互式菜单（推荐使用）${NC}"
        echo -e "  ${GREEN}$0 start urls${NC}       ${PURPLE}# 前台启动并打印组件URL${NC}"
        echo -e "  ${GREEN}$0 daemon urls${NC}      ${PURPLE}# 后台启动并打印组件URL${NC}"
        echo -e "  ${GREEN}$0 debug urls 5005${NC}  ${PURPLE}# 调试模式并打印组件URL${NC}"
        echo -e "  ${GREEN}$0 cleanup${NC}          ${PURPLE}# 手动清理提示进程${NC}"
        echo ""
        echo -e "${BOLD}${YELLOW}💡 提示: 使用 ${GREEN}$0 -i${NC} ${YELLOW}进入交互式菜单，体验更好！${NC}"
        exit 1
        ;;
esac

exit $?
