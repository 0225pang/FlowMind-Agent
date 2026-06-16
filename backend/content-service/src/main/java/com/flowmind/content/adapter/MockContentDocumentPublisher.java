package com.flowmind.content.adapter;

import com.flowmind.content.dto.ContentGenerateResponse;
import com.flowmind.content.port.ContentDocumentPublisher;
import org.springframework.stereotype.Component;

@Component
public class MockContentDocumentPublisher implements ContentDocumentPublisher {
    @Override
    public String publish(ContentGenerateResponse response) {
        return "mock-feishu-doc-" + response.getAgentType();
    }
}
