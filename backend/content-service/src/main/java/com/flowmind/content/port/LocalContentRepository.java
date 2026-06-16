package com.flowmind.content.port;

import com.flowmind.content.dto.ContentGenerateResponse;

public interface LocalContentRepository {
    void save(ContentGenerateResponse response);
}
