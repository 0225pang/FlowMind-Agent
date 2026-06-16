package com.flowmind.knowledge.config;

import com.flowmind.knowledge.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSyncInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSyncInitializer.class);

    private final KnowledgeService knowledgeService;

    public KnowledgeSyncInitializer(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Auto-syncing knowledge from Feishu on startup...");
        try {
            var result = knowledgeService.syncFromFeishu();
            log.info("Startup sync result: {}", result);
        } catch (Exception e) {
            log.warn("Startup sync failed (non-fatal): {}", e.getMessage());
        }
    }
}
