@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

rem Super Agent Platform 启动脚本 - Windows版本
rem 使用方法: startup.bat [start|stop|restart|status]

set APP_NAME=super-agent-boot
set APP_JAR=lib\%APP_NAME%.jar
set PID_FILE=logs\%APP_NAME%.pid
set LOG_FILE=logs\%APP_NAME%.log

rem 获取脚本所在目录
set APP_HOME=%~dp0..
cd /d "%APP_HOME%"

rem JVM参数
set JVM_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/
set SPRING_OPTS=--spring.config.location=conf/application.yml --logging.file.path=logs/

rem 检查Java环境
if defined JAVA_HOME (
    set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_CMD=java
)

rem 检查参数
if "%1"=="" goto usage
if "%1"=="start" goto start
if "%1"=="stop" goto stop
if "%1"=="restart" goto restart
if "%1"=="status" goto status
goto usage

:check_java
%JAVA_CMD% -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Java运行环境，请安装JDK 17+
    exit /b 1
)
goto :eof

:start
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    tasklist /fi "pid eq !PID!" 2>nul | find "!PID!" >nul
    if not errorlevel 1 (
        echo ⚠️  应用已在运行中，PID: !PID!
        goto :eof
    ) else (
        echo 🔄 清理无效的PID文件
        del /f /q "%PID_FILE%" 2>nul
    )
)

call :check_java

echo 🚀 启动 %APP_NAME%...
if not exist logs mkdir logs

start /b "" %JAVA_CMD% %JVM_OPTS% -jar "%APP_JAR%" %SPRING_OPTS% > "%LOG_FILE%" 2>&1

rem 获取Java进程PID（简化版本，实际可能需要更复杂的逻辑）
timeout /t 3 /nobreak >nul
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo csv ^| find "%APP_NAME%"') do (
    set PID=%%i
)

if defined PID (
    echo !PID! > "%PID_FILE%"
    echo ✅ %APP_NAME% 启动成功，PID: !PID!
    echo 📖 API文档: http://localhost:8080/doc.html
    echo 📊 监控页面: http://localhost:8080/druid
    echo 📝 日志文件: %LOG_FILE%
) else (
    echo ❌ %APP_NAME% 启动失败，请检查日志: %LOG_FILE%
)
goto :eof

:stop
if not exist "%PID_FILE%" (
    echo ⚠️  应用未运行
    goto :eof
)

set /p PID=<"%PID_FILE%"
tasklist /fi "pid eq %PID%" 2>nul | find "%PID%" >nul
if errorlevel 1 (
    echo ⚠️  应用未运行，清理PID文件
    del /f /q "%PID_FILE%" 2>nul
    goto :eof
)

echo 🛑 停止 %APP_NAME% (PID: %PID%)...
taskkill /pid %PID% /f >nul 2>&1
if not errorlevel 1 (
    echo ✅ %APP_NAME% 已停止
    del /f /q "%PID_FILE%" 2>nul
) else (
    echo ❌ 停止 %APP_NAME% 失败
)
goto :eof

:status
if not exist "%PID_FILE%" (
    echo 📋 状态: %APP_NAME% 未运行
    goto :eof
)

set /p PID=<"%PID_FILE%"
tasklist /fi "pid eq %PID%" 2>nul | find "%PID%" >nul
if not errorlevel 1 (
    echo 📋 状态: %APP_NAME% 正在运行，PID: %PID%
) else (
    echo 📋 状态: %APP_NAME% 未运行（PID文件存在但进程不存在）
    del /f /q "%PID_FILE%" 2>nul
)
goto :eof

:restart
echo 🔄 重启 %APP_NAME%...
call :stop
timeout /t 2 /nobreak >nul
call :start
goto :eof

:usage
echo 用法: %0 {start^|stop^|restart^|status}
echo.
echo 命令说明:
echo   start   - 启动应用
echo   stop    - 停止应用
echo   restart - 重启应用
echo   status  - 查看运行状态
echo.
pause
exit /b 1

:eof
