package com.flowmind.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wraps lark-cli command execution for AI agent tool calling.
 *
 * <p>On Windows, uses "cmd /c lark-cli.cmd ..." to properly invoke the npm
 * global script. Sets the working directory to a temp folder so that
 * {@code --content @doc.md} resolves as a relative path (lark-cli policy).
 */
@Service
public class LarkCliToolService {
    private static final Logger log = LoggerFactory.getLogger(LarkCliToolService.class);

    @Value("${flowmind.tools.lark-cli.command:lark-cli}")
    private String command;

    @Value("${flowmind.tools.lark-cli.timeout-seconds:30}")
    private long timeoutSeconds;

    @Value("${flowmind.tools.lark-cli.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Public API ──

    public JsonNode createDoc(String title, String content, String parentToken, String asIdentity,
                               Consumer<String> onProgress) throws Exception {
        if (!enabled) throw new IllegalStateException("lark-cli not enabled");

        String fullMd = "# " + title + "\n\n" + (content != null ? content : "");
        Path workDir = Files.createTempDirectory("flowmind-lark-");
        Path mdFile = workDir.resolve("doc.md");
        Files.writeString(mdFile, fullMd, StandardCharsets.UTF_8);

        List<String> args = baseArgs();
        args.add("docs");
        args.add("+create");
        args.add("--api-version");
        args.add("v2");
        args.add("--doc-format");
        args.add("markdown");
        args.add("--format");
        args.add("json");
        args.add("--content");
        args.add("@doc.md");
        args.add("--as");
        args.add(asIdentity != null && !asIdentity.isBlank() ? asIdentity : "user");
        if (parentToken != null && !parentToken.isBlank()) {
            args.add("--parent-token");
            args.add(parentToken);
        }

        onProgress.accept("Creating Feishu doc: " + title + "\n");
        log.info("lark-cli create: title={}, workDir={}", title, workDir);

        try { return execute(workDir, args, onProgress); }
        finally { cleanup(workDir, mdFile); }
    }

    public JsonNode fetchDoc(String docToken, String asIdentity) throws Exception {
        if (!enabled) throw new IllegalStateException("lark-cli not enabled");
        List<String> args = baseArgs();
        args.add("docs");
        args.add("+fetch");
        args.add("--api-version");
        args.add("v2");
        args.add("--format");
        args.add("json");
        args.add("--as");
        args.add(asIdentity != null && !asIdentity.isBlank() ? asIdentity : "user");
        args.add("--doc");
        args.add(docToken);
        return execute(null, args, null);
    }

    public JsonNode listFolder(String folderToken, String asIdentity) throws Exception {
        if (!enabled) throw new IllegalStateException("lark-cli not enabled");
        if (folderToken == null || folderToken.isBlank()) {
            throw new IllegalArgumentException("folderToken is required");
        }
        List<String> args = baseArgs();
        args.add("drive");
        args.add("files");
        args.add("list");
        args.add("--params");
        args.add("-");
        args.add("--format");
        args.add("json");
        args.add("--as");
        args.add(asIdentity != null && !asIdentity.isBlank() ? asIdentity : "user");
        String params = "{\"folder_token\":\"" + escapeJson(folderToken) + "\",\"page_size\":200}";
        return execute(null, args, null, params);
    }

    public String checkVersion() {
        try {
            List<String> args = baseArgs();
            args.add("--version");
            return execute(null, args, null).toString();
        } catch (Exception e) {
            return "unavailable: " + e.getMessage();
        }
    }

    public boolean isAvailable() {
        if (!enabled) return false;
        try { checkVersion(); return true; } catch (Exception e) { return false; }
    }

    /**
     * Download a Drive file (PDF, etc.) to a local path using {@code lark-cli drive +download}.
     * Uses the given outputDir as working directory and the fileToken as relative filename.
     * Returns the local file path.
     */
    public Path downloadFile(String fileToken, Path outputDir) throws Exception {
        if (!enabled) throw new IllegalStateException("lark-cli not enabled");
        String fileName = fileToken + ".pdf";
        List<String> args = baseArgs();
        args.add("drive");
        args.add("+download");
        args.add("--file-token");
        args.add(fileToken);
        args.add("--output");
        args.add(fileName);                  // relative path, lark-cli requires this
        args.add("--overwrite");
        args.add("--format");
        args.add("json");
        args.add("--as");
        args.add("user");
        execute(outputDir, args, null);
        Path out = outputDir.resolve(fileName);
        return Files.exists(out) ? out : null;
    }

    // ── Internal ──

    /**
     * Base args: on Windows we need {@code cmd /c lark-cli ...} so that
     * {@code lark-cli.cmd} (npm global script) is found via PATH.
     * On Unix we just call {@code lark-cli} directly.
     */
    private List<String> baseArgs() {
        List<String> args = new ArrayList<>();
        if (isWindows()) {
            args.add("cmd");
            args.add("/c");
            args.add(command);       // lark-cli (resolves to lark-cli.cmd via PATHEXT)
        } else {
            args.add(command);
        }
        return args;
    }

    private JsonNode execute(Path workDir, List<String> args, Consumer<String> onProgress) throws Exception {
        return execute(workDir, args, onProgress, null);
    }

    private JsonNode execute(Path workDir, List<String> args, Consumer<String> onProgress, String stdin) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args);
        if (workDir != null) builder.directory(workDir.toFile());
        builder.environment().put("LARK_CLI_NO_PROXY", "1");
        builder.redirectErrorStream(true);

        Process process = builder.start();
        if (stdin != null) {
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(stdin);
                writer.flush();
            }
        } else {
            process.getOutputStream().close();
        }
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                if (onProgress != null) onProgress.accept(line + "\n");
            }
        }

        boolean finished = process.waitFor(Duration.ofSeconds(timeoutSeconds).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new Exception("lark-cli timeout (" + timeoutSeconds + "s)");
        }

        int code = process.exitValue();
        String text = output.toString().trim();
        log.debug("lark-cli exit={} out={}", code, truncate(text, 200));

        if (code != 0) {
            throw new Exception("lark-cli exit " + code + ": " + truncate(text, 500));
        }
        if (text.isEmpty()) {
            return objectMapper.createObjectNode().put("status", "ok");
        }
        try {
            return objectMapper.readTree(extractJson(text));
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("raw", text).put("status", "ok");
        }
    }

    private void cleanup(Path workDir, Path mdFile) {
        try { Files.deleteIfExists(mdFile); } catch (Exception ignored) {}
        try { Files.deleteIfExists(workDir); } catch (Exception ignored) {}
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJson(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start;
        if (objectStart < 0) {
            start = arrayStart;
        } else if (arrayStart < 0) {
            start = objectStart;
        } else {
            start = Math.min(objectStart, arrayStart);
        }
        return start >= 0 ? trimmed.substring(start) : trimmed;
    }
}
