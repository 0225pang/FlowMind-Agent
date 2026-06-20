package com.flowmind.agent.extension.xiaohongshu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class XiaohongshuMcpProcessManager implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(XiaohongshuMcpProcessManager.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "xiaohongshu-mcp-log-reader");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${flowmind.tools.xiaohongshu-mcp.enabled:false}")
    private boolean enabled;

    @Value("${flowmind.tools.xiaohongshu-mcp.auto-start:false}")
    private boolean autoStart;

    @Value("${flowmind.tools.xiaohongshu-mcp.command:go}")
    private String command;

    @Value("${flowmind.tools.xiaohongshu-mcp.working-dir:ai-agent-service/integrations/xiaohongshu-mcp}")
    private String workingDir;

    @Value("${flowmind.tools.xiaohongshu-mcp.port:18060}")
    private int port;

    @Value("${flowmind.tools.xiaohongshu-mcp.headless:true}")
    private boolean headless;

    @Value("${flowmind.tools.xiaohongshu-mcp.browser-bin:}")
    private String browserBin;

    @Value("${flowmind.tools.xiaohongshu-mcp.chrome-user-data-dir:.runtime/xiaohongshu/chrome-profile}")
    private String chromeUserDataDir;

    @Value("${flowmind.tools.xiaohongshu-mcp.cookies-path:.runtime/xiaohongshu/cookies.json}")
    private String cookiesPath;

    @Value("${flowmind.tools.xiaohongshu-mcp.go-cache:.gocache/xiaohongshu-mcp}")
    private String goCache;

    @Value("${flowmind.tools.xiaohongshu-mcp.base-url:http://localhost:18060}")
    private String baseUrl;

    private volatile Process process;
    private volatile boolean running;

    @Override
    public void start() {
        if (!enabled || !autoStart) {
            log.info("Xiaohongshu MCP auto-start is disabled.");
            return;
        }
        if (isHealthy()) {
            running = true;
            log.info("Xiaohongshu MCP is already running at {}", baseUrl);
            return;
        }
        Path directory = resolveWorkingDirectory();
        if (directory == null) {
            log.warn("Xiaohongshu MCP working directory was not found. Expected: {}", workingDir);
            return;
        }
        try {
            List<String> commandLine = new ArrayList<>();
            commandLine.add(command);
            commandLine.add("run");
            commandLine.add(".");
            commandLine.add("-port");
            commandLine.add(":" + port);
            commandLine.add("-headless=" + headless);
            if (browserBin != null && !browserBin.isBlank()) {
                Path resolvedBrowserBin = resolveProjectPath(browserBin);
                commandLine.add("-bin");
                commandLine.add(resolvedBrowserBin.toString());
            }

            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.directory(directory.toFile());
            builder.redirectErrorStream(true);
            configureGoEnvironment(builder.environment());
            configureBrowserEnvironment(builder.environment());
            process = builder.start();
            running = true;
            log.info("Started Xiaohongshu MCP from {} on port {}", directory, port);
            logExecutor.submit(() -> pipeProcessLog(process));
        } catch (IOException e) {
            running = false;
            log.warn("Failed to start Xiaohongshu MCP. Install Go 1.24+ or set flowmind.tools.xiaohongshu-mcp.auto-start=false. Cause: {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        Process current = process;
        if (current != null && current.isAlive()) {
            current.destroy();
            try {
                if (!current.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    current.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                current.destroyForcibly();
            }
        }
        logExecutor.shutdownNow();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running && (process == null || process.isAlive() || isHealthy());
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE + 200;
    }

    private boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimBase(baseUrl) + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Path resolveWorkingDirectory() {
        List<Path> candidates = List.of(
                Path.of(workingDir),
                Path.of("backend").resolve(workingDir),
                Path.of("..").resolve(workingDir),
                Path.of("..").resolve("ai-agent-service").resolve("integrations").resolve("xiaohongshu-mcp")
        );
        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(absolute) && Files.exists(absolute.resolve("go.mod"))) {
                return absolute;
            }
        }
        return null;
    }

    private void configureGoEnvironment(Map<String, String> environment) {
        if (goCache == null || goCache.isBlank()) return;
        Path cachePath = Path.of(goCache);
        if (!cachePath.isAbsolute()) {
            cachePath = resolveProjectPath(goCache);
        }
        try {
            Files.createDirectories(cachePath);
            environment.put("GOCACHE", cachePath.toString());
            log.info("Xiaohongshu MCP uses Go build cache: {}", cachePath);
        } catch (IOException e) {
            log.warn("Failed to create Xiaohongshu MCP Go cache at {}: {}", cachePath, e.getMessage());
        }
    }

    private void configureBrowserEnvironment(Map<String, String> environment) {
        if (browserBin != null && !browserBin.isBlank()) {
            Path resolvedBrowserBin = resolveProjectPath(browserBin);
            environment.put("ROD_BROWSER_BIN", resolvedBrowserBin.toString());
            log.info("Xiaohongshu MCP uses browser binary: {}", resolvedBrowserBin);
        }
        configureRuntimePath(environment, "XHS_CHROME_USER_DATA_DIR", chromeUserDataDir, true);
        configureRuntimePath(environment, "COOKIES_PATH", cookiesPath, false);
    }

    private void configureRuntimePath(Map<String, String> environment, String key, String value, boolean directory) {
        if (value == null || value.isBlank()) return;
        Path path = resolveProjectPath(value);
        try {
            if (directory) {
                Files.createDirectories(path);
            } else {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            }
            environment.put(key, path.toString());
            log.info("Xiaohongshu MCP uses {}: {}", key, path);
        } catch (IOException e) {
            log.warn("Failed to prepare Xiaohongshu MCP runtime path {}={}: {}", key, path, e.getMessage());
        }
    }

    private Path resolveProjectPath(String value) {
        Path path = Path.of(value.trim());
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return projectRoot().resolve(path).normalize();
    }

    private Path projectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path current = cwd;
        while (current != null) {
            if (Files.isDirectory(current.resolve("backend")) && Files.isDirectory(current.resolve("docs"))) {
                return current;
            }
            if ("backend".equalsIgnoreCase(String.valueOf(current.getFileName())) && current.getParent() != null) {
                return current.getParent();
            }
            current = current.getParent();
        }
        return cwd;
    }

    private void pipeProcessLog(Process current) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(current.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[xiaohongshu-mcp] {}", line);
            }
        } catch (IOException e) {
            log.debug("Xiaohongshu MCP log reader stopped: {}", e.getMessage());
        }
    }

    private String trimBase(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
