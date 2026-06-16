package com.flowmind.content.port;

import com.flowmind.content.dto.ContentGenerateRequest;

import java.util.List;
import java.util.Map;

public interface ContentGenerationClient {
    List<Map<String, Object>> generateVersions(ContentGenerateRequest request, Map<String, Object> template);

    List<String> generateTitles(ContentGenerateRequest request, Map<String, Object> template);
}
