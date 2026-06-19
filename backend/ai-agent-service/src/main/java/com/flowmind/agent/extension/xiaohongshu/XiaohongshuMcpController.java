package com.flowmind.agent.extension.xiaohongshu;

import com.flowmind.common.core.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agents/xiaohongshu")
public class XiaohongshuMcpController {
    private final XiaohongshuMcpClient client;
    private final XiaohongshuMcpToolRegistry registry;

    public XiaohongshuMcpController(XiaohongshuMcpClient client, XiaohongshuMcpToolRegistry registry) {
        this.client = client;
        this.registry = registry;
    }

    @GetMapping("/tools")
    public ApiResponse<?> tools() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("agentVisible", registry.exposedTools());
        body.put("disabledInternal", registry.disabledTools());
        body.put("note", "Only read/search tools are visible to FlowMind Agent. Publish/comment/like/collect tools are registered but disabled.");
        return ApiResponse.success(body);
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@RequestParam String topic,
                                 @RequestParam(defaultValue = "8") int limit) {
        XiaohongshuMcpClient.SearchResult result = client.searchHotNotes(topic, limit);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", result.topic());
        body.put("mode", result.mode());
        body.put("message", result.message());
        body.put("notes", result.notes().stream().map(XiaohongshuMcpClient.HotNote::toMap).toList());
        return ApiResponse.success(body);
    }

    @GetMapping("/detail")
    public ApiResponse<?> detail(@RequestParam String id) {
        return ApiResponse.success(client.readNoteDetail(id));
    }
}
