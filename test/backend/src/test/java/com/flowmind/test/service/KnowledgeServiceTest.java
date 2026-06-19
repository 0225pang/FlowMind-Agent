package com.flowmind.test.service;

import com.flowmind.knowledge.entity.KnowledgeDocEntity;
import com.flowmind.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("KnowledgeService — Unit Tests")
class KnowledgeServiceTest extends BaseServiceTest {

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(
                knowledgeMapper, larkCliToolService, mockLlmClient,
                syncLogMapper, embeddingService, weaviateClient);
        when(weaviateClient.isEnabled()).thenReturn(false);
    }

    @Nested
    @DisplayName("searchDocs")
    class SearchDocs {
        @Test @DisplayName("should delegate to mapper with keyword")
        void shouldDelegateToMapper() {
            when(knowledgeMapper.search("面试")).thenReturn(List.of(
                    mockDoc(1L, "tok1", "面试指南", "content1", "summary1")
            ));
            List<KnowledgeDocEntity> result = knowledgeService.searchDocs("面试");
            assertEquals(1, result.size());
            assertEquals("面试指南", result.get(0).getTitle());
            verify(knowledgeMapper).search("面试");
        }

        @Test @DisplayName("should return empty list when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            when(knowledgeMapper.search(anyString())).thenReturn(List.of());
            List<KnowledgeDocEntity> result = knowledgeService.searchDocs("nonexistent");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getDoc")
    class GetDoc {
        @Test @DisplayName("should return doc by id")
        void shouldReturnDocById() {
            KnowledgeDocEntity doc = mockDoc(42L, "tok42", "Test Doc", "content", "summary");
            when(knowledgeMapper.findById(42L)).thenReturn(Optional.of(doc));
            Optional<KnowledgeDocEntity> result = knowledgeService.getDoc(42L);
            assertTrue(result.isPresent());
            assertEquals("Test Doc", result.get().getTitle());
        }

        @Test @DisplayName("should return empty optional for missing id")
        void shouldReturnEmptyForMissingId() {
            when(knowledgeMapper.findById(99999L)).thenReturn(Optional.empty());
            Optional<KnowledgeDocEntity> result = knowledgeService.getDoc(99999L);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("docCount")
    class DocCount {
        @Test @DisplayName("should return mapper count")
        void shouldReturnMapperCount() {
            when(knowledgeMapper.count()).thenReturn(17L);
            assertEquals(17L, knowledgeService.docCount());
        }

        @Test @DisplayName("should return 0 for empty DB")
        void shouldReturnZeroForEmptyDb() {
            when(knowledgeMapper.count()).thenReturn(0L);
            assertEquals(0L, knowledgeService.docCount());
        }
    }

    @Nested
    @DisplayName("updateTags")
    class UpdateTags {
        @Test @DisplayName("should update tags via mapper")
        void shouldUpdateTagsViaMapper() {
            KnowledgeDocEntity doc = mockDoc(1L, "tok1", "Doc", "content", "summary");
            when(knowledgeMapper.findById(1L)).thenReturn(Optional.of(doc));
            KnowledgeDocEntity result = knowledgeService.updateTags(1L, List.of("夏令营", "面试"));
            assertNotNull(result);
            verify(knowledgeMapper).updateTags(eq(1L), contains("\"夏令营\""));
        }

        @Test @DisplayName("should handle empty tags list")
        void shouldHandleEmptyTags() {
            KnowledgeDocEntity doc = mockDoc(1L, "tok1", "Doc", "content", "summary");
            when(knowledgeMapper.findById(1L)).thenReturn(Optional.of(doc));
            KnowledgeDocEntity result = knowledgeService.updateTags(1L, List.of());
            assertNotNull(result);
            verify(knowledgeMapper).updateTags(eq(1L), eq("[]"));
        }
    }

    @Nested
    @DisplayName("syncFromFeishu — edge cases")
    class Sync {
        @Test @DisplayName("should handle lark-cli not available")
        void shouldHandleLarkCliUnavailable() {
            when(larkCliToolService.isAvailable()).thenReturn(false);
            Map<String, Object> result = knowledgeService.syncFromFeishu();
            assertEquals("error", result.get("status"));
            assertEquals(0, (int) result.get("added"));
            assertEquals(0, (int) result.get("updated"));
            assertEquals(1, (int) result.get("errors"));
        }
    }
}
