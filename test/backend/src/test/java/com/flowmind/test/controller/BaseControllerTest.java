package com.flowmind.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for endpoint-level integration tests.
 * Uses H2 in-memory DB. Real controllers + real Spring context.
 */
@SpringBootTest(classes = com.flowmind.app.FlowMindApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String AUTH_HEADER = "Authorization";
    protected static final String BEARER_TOKEN = "Bearer mock-jwt.admin";

    @BeforeEach
    void baseSetUp() {
        // H2 auto-creates tables; no-op for subclasses
    }
}
