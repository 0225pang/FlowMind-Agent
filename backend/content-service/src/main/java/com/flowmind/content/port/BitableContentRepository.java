package com.flowmind.content.port;

import com.flowmind.content.dto.ContentGenerateResponse;

public interface BitableContentRepository {
    void saveStructuredRecord(ContentGenerateResponse response);
}
