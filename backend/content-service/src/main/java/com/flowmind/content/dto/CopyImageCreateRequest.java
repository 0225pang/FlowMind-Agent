package com.flowmind.content.dto;

public record CopyImageCreateRequest(
        String name,
        String url,
        String storageProvider,
        String objectKey
) {
}
