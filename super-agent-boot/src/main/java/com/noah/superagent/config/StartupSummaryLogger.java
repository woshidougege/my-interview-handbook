package com.noah.superagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import cn.hutool.core.net.NetUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 自定义字节大小格式化工具

public class StartupSummaryLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupSummaryLogger.class);

    /**
     * 格式化字节大小为人类可读的格式
     */
    private static String formatSize(long bytes) {
        if (bytes < 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        if (unitIndex == 0) {
            return String.format("%.0f %s", size, units[unitIndex]);
        } else {
            return String.format("%.1f %s", size, units[unitIndex]);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();

        boolean color = useColor(env);
        String C_TITLE = color ? "\u001B[96m" : "";   // bright cyan
        String C_LABEL = color ? "\u001B[36m" : "";   // cyan
        String C_VALUE = color ? "\u001B[92m" : "";   // bright green
        String C_DIM   = color ? "\u001B[90m" : "";   // dim gray
        String C_URL   = color ? "\u001B[94m" : "";   // bright blue for URLs
        String C_RESET = color ? "\u001B[0m"  : "";

        String appName = env.getProperty("spring.application.name", "Super Agnet");
        String version = env.getProperty("info.app.version", env.getProperty("APP_VERSION", "1.0.0"));
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profiles = String.join(", ", env.getActiveProfiles().length == 0 ? new String[]{"default"} : env.getActiveProfiles());
        String logPath = env.getProperty("logging.file.path", "logs");
        String pid = ManagementFactory.getRuntimeMXBean().getName();

        // Memory (heap)
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        long heapUsed = heap.getUsed();
        long heapMax  = heap.getMax();

        // OS / CPU / Physical memory
        String osName = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String jvmVendor = System.getProperty("java.vendor");
        int cpuCores = Runtime.getRuntime().availableProcessors();

        long totalPhys = -1L; // bytes
        long freePhys  = -1L; // bytes
        try {
            com.sun.management.OperatingSystemMXBean osm =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            totalPhys = osm.getTotalPhysicalMemorySize();
            freePhys  = osm.getFreePhysicalMemorySize();
        } catch (Throwable ignored) { }

        // Disk summary (bytes) - 只检查当前工作目录所在的磁盘，避免遍历所有磁盘
        long diskTotal = 0, diskFree = 0;
        try {
            File currentDir = new File(".");
            diskTotal = currentDir.getTotalSpace();
            diskFree = currentDir.getUsableSpace();
        } catch (Throwable ignored) { }

        // Uptime
        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        long now = System.currentTimeMillis();
        String uptime = Duration.ofMillis(now - startTime).toString().replace("PT", "").toLowerCase();

        // Hosts - 使用Hutool快速获取网络IP
        String lanHost = NetUtil.getLocalhostStr();
        String scheme = env.getProperty("server.ssl.enabled", "false").equals("true") ? "https" : "http";
        String localBase = String.format("%s://localhost:%s%s", scheme, port, contextPath);
        String netBase   = String.format("%s://%s:%s%s", scheme, lanHost, port, contextPath);

        // Config summary line
        ConfigSummary configSummary = collectConfigSummary(env);
        String configsLine = buildConfigsLine(configSummary);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=============================================================\n");
        sb.append(String.format("%s🚀 %s v%s Started Successfully!%s%n", C_TITLE, appName, version, C_RESET));
        sb.append("-------------------------------------------------------------\n");
        sb.append(String.format("%s• Profiles       %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + profiles, C_RESET));
        if (!configsLine.isEmpty()) {
            sb.append(String.format("%s• Configs        %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + configsLine, C_RESET));
        }
        sb.append(String.format("%s• Local          %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + localBase, C_RESET));
        sb.append(String.format("%s• Network        %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + netBase, C_RESET));

        // Condensed endpoints (only when URLS is on), with explanations
        if (isOn(env.getProperty("URLS", "false"))) {
            sb.append(String.format("%s  ◦ Endpoints    %s:%n", C_DIM, C_RESET));
            sb.append(String.format("    - %s%-14s%s %s%s%s%n", C_URL, "/swagger-ui.html", C_RESET, C_VALUE, "接口文档与调试 (Swagger UI)", C_RESET));
            sb.append(String.format("    - %s%-14s%s %s%s%s%n", C_URL, "/v3/api-docs", C_RESET, C_VALUE, "OpenAPI JSON 定义", C_RESET));
            sb.append(String.format("    - %s%-14s%s %s%s%s%n", C_URL, "/actuator", C_RESET, C_VALUE, "应用监控端点 (Spring Actuator)", C_RESET));
            sb.append(String.format("    - %s%-14s%s %s%s%s%n", C_URL, "/druid", C_RESET, C_VALUE, "数据源与SQL监控 (Druid)", C_RESET));
        }

        sb.append(String.format("%s• Java/JVM       %s: %s (%s)%s%n", C_LABEL, C_RESET, C_VALUE + javaVersion + C_RESET, jvmVendor, ""));
        sb.append(String.format("%s• OS             %s: %s (%s)%s%n", C_LABEL, C_RESET, C_VALUE + osName + C_RESET, osArch, ""));
        sb.append(String.format("%s• CPU            %s: %s%d cores%s%n", C_LABEL, C_RESET, C_VALUE, cpuCores, C_RESET));
        // Physical memory (prefer), with heap detail as supplement
        if (totalPhys > 0 && freePhys >= 0) {
            long usedPhys = totalPhys - freePhys;
            String physStr = formatSize(usedPhys) + " used / " + formatSize(totalPhys) + " total";
            String heapStr = formatSize(heapUsed) + " heap / " + formatSize(heapMax) + " heap max";
            sb.append(String.format("%s• Memory         %s: %s%s (%s)%s%n", C_LABEL, C_RESET, C_VALUE, physStr, heapStr, C_RESET));
        } else {
            String heapStr = formatSize(heapUsed) + " heap / " + formatSize(heapMax) + " heap max";
            sb.append(String.format("%s• Memory         %s: %s%s%s%n", C_LABEL, C_RESET, C_VALUE, heapStr, C_RESET));
        }
        if (diskTotal > 0) {
            long usedDisk = diskTotal - diskFree;
            String diskStr = formatSize(usedDisk) + " used / " + formatSize(diskTotal) + " total";
            sb.append(String.format("%s• Disk           %s: %s%s%s%n", C_LABEL, C_RESET, C_VALUE, diskStr, C_RESET));
        }

        sb.append(String.format("%s• Logs Dir       %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + logPath, C_RESET));
        sb.append(String.format("%s• PID            %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + pid, C_RESET));
        sb.append(String.format("%s• Uptime         %s: %s%s%n", C_LABEL, C_RESET, C_VALUE + uptime, C_RESET));
        sb.append("=============================================================\n");

        log.info(sb.toString());
    }

    private boolean useColor(ConfigurableEnvironment env) {
        String v = env.getProperty("spring.output.ansi.enabled", "always");
        return !"never".equalsIgnoreCase(v);
    }

    private boolean isOn(String v) {
        if (v == null) return false;
        String s = v.trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s) || "on".equals(s);
    }

    private static class ConfigSummary {
        boolean hasMain;
        String ext; // yml|yaml
        List<String> tokens = new ArrayList<>();
    }

    private ConfigSummary collectConfigSummary(ConfigurableEnvironment env) {
        ConfigSummary cs = new ConfigSummary();
        cs.ext = "yml"; // default
        Set<String> set = new LinkedHashSet<>();
        
        // 限制遍历数量，避免在PropertySources很多时耗时过长
        int count = 0;
        for (PropertySource<?> ps : env.getPropertySources()) {
            if (++count > 50) break; // 限制最多检查50个PropertySource
            
            String name = ps.getName();
            if (name.startsWith("Config resource '") && name.contains("application")) {
                int lb = name.indexOf('[');
                int rb = name.indexOf(']', lb + 1);
                if (lb > 0 && rb > lb) {
                    String file = name.substring(lb + 1, rb).toLowerCase();
                    if (file.endsWith(".yaml")) cs.ext = "yaml";
                    if (file.equals("application.yml") || file.equals("application.yaml")) {
                        cs.hasMain = true;
                        continue;
                    }
                    if (file.startsWith("application-") && (file.endsWith(".yml") || file.endsWith(".yaml"))) {
                        String token = file.substring("application-".length());
                        token = token.replaceFirst("\\.ya?ml$", "");
                        token = token.replace('_', '-');
                        set.add(token);
                    }
                }
            }
        }
        cs.tokens.addAll(set);
        Collections.sort(cs.tokens);
        return cs;
    }

    private String buildConfigsLine(ConfigSummary cs) {
        String ext = cs.ext == null ? "yml" : cs.ext;
        StringBuilder line = new StringBuilder();
        if (cs.hasMain) {
            line.append("application.").append(ext);
            if (!cs.tokens.isEmpty()) line.append(", ");
        }
        if (!cs.tokens.isEmpty()) {
            line.append("application-[");
            for (int i = 0; i < cs.tokens.size(); i++) {
                line.append(cs.tokens.get(i));
                if (i < cs.tokens.size() - 1) line.append(", ");
            }
            line.append("].").append(ext);
        }
        return line.toString();
    }

}
