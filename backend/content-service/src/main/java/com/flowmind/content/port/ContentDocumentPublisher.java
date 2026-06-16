package com.flowmind.content.port;

import com.flowmind.content.dto.ContentGenerateResponse;

public interface ContentDocumentPublisher {
    String publish(ContentGenerateResponse response);
}
