package com.flowmind.knowledge.config;

import com.flowmind.knowledge.service.KnowledgeService;
import com.flowmind.knowledge.vector.WeaviateClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSyncInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSyncInitializer.class);

    private final KnowledgeService knowledgeService;
    private final WeaviateClientService weaviateClient;

    public KnowledgeSyncInitializer(KnowledgeService knowledgeService, WeaviateClientService weaviateClient) {
        this.knowledgeService = knowledgeService;
        this.weaviateClient = weaviateClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Ensure Weaviate schema before syncing
        weaviateClient.ensureSchema();

        log.info("Auto-syncing knowledge from Feishu on startup...");
        try {
            var result = knowledgeService.syncFromFeishu();
            log.info("Startup sync result: {}", result);
            var reindexResult = knowledgeService.reindexMissingWeaviateDocs();
            log.info("Startup missing Weaviate reindex result: {}", reindexResult);
        } catch (Exception e) {
            log.warn("Startup sync failed (non-fatal): {}", e.getMessage());
        }
    }
}
