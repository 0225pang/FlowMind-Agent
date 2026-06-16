package com.flowmind.content.port;

import com.flowmind.content.dto.ContentGenerateRequest;

import java.util.List;
import java.util.Map;

public interface KnowledgeRetriever {
    List<Map<String, Object>> retrieveStructures(ContentGenerateRequest request);
}
