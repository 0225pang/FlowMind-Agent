package com.flowmind.knowledge.vector;

import com.flowmind.common.core.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge/vector")
public class VectorSearchController {
    private final VectorSearchToolService vectorSearchToolService;

    public VectorSearchController(VectorSearchToolService vectorSearchToolService) {
        this.vectorSearchToolService = vectorSearchToolService;
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@RequestParam String q,
                                 @RequestParam(defaultValue = "5") int topK) {
        return ApiResponse.success(vectorSearchToolService.search(q, topK));
    }
}
