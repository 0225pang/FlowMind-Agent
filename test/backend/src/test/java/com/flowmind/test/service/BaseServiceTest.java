package com.flowmind.test.service;

import com.flowmind.agent.llm.MockLLMClient;
import com.flowmind.agent.service.LarkCliToolService;
import com.flowmind.knowledge.mapper.KnowledgeMapper;
import com.flowmind.knowledge.mapper.SyncLogMapper;
import com.flowmind.knowledge.vector.EmbeddingService;
import com.flowmind.knowledge.vector.WeaviateClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for service-level unit tests.
 * Mocks external dependencies (DB, Weaviate, Lark cli, LLM).
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    @Mock
    protected JdbcTemplate jdbcTemplate;

    @Mock
    protected KnowledgeMapper knowledgeMapper;

    @Mock
    protected SyncLogMapper syncLogMapper;

    @Mock
    protected LarkCliToolService larkCliToolService;

    @Mock
    protected EmbeddingService embeddingService;

    @Mock
    protected WeaviateClientService weaviateClient;

    protected MockLLMClient mockLlmClient;

    @BeforeEach
    void baseSetUp() {
        mockLlmClient = new MockLLMClient();
    }

    /** Build a mock KnowledgeDocEntity with given fields */
    protected com.flowmind.knowledge.entity.KnowledgeDocEntity mockDoc(
            long id, String token, String title, String content, String summary) {
        com.flowmind.knowledge.entity.KnowledgeDocEntity doc =
                new com.flowmind.knowledge.entity.KnowledgeDocEntity();
        doc.setId(id);
        doc.setFeishuToken(token);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSummary(summary);
        doc.setFeishuUrl("https://test.feishu.cn/docx/" + token);
        doc.setFeishuType("docx");
        doc.setFeishuModifiedAt(1780000000L);
        doc.setTags("[\"test\"]");
        return doc;
    }

    protected com.flowmind.knowledge.entity.SyncLogEntity mockSyncLog(
            long id, String syncType, String status, int added, int updated, int skipped) {
        com.flowmind.knowledge.entity.SyncLogEntity log =
                new com.flowmind.knowledge.entity.SyncLogEntity();
        log.setId(id);
        log.setSyncType(syncType);
        log.setStatus(status);
        log.setMessage("Test sync log");
        log.setAdded(added);
        log.setUpdated(updated);
        log.setSkipped(skipped);
        log.setErrors(0);
        return log;
    }
}
