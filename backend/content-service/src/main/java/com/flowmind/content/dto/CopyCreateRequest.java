package com.flowmind.content.dto;

public record CopyCreateRequest(
        String title,
        String channel,
        String version,
        String style,
        String content,
        String owner
) {
}
