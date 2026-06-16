package com.flowmind.content.adapter;

import com.flowmind.content.dto.ContentGenerateResponse;
import com.flowmind.content.port.BitableContentRepository;
import org.springframework.stereotype.Component;

@Component
public class MockBitableContentRepository implements BitableContentRepository {
    @Override
    public void saveStructuredRecord(ContentGenerateResponse response) {
        // Demo: later replace with Feishu bitable OpenAPI implementation.
    }
}
