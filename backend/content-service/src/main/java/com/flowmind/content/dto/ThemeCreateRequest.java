package com.flowmind.content.dto;

import java.util.List;

public record ThemeCreateRequest(
        String title,
        String topic,
        String platform,
        String type,
        String status,
        Integer heat,
        String plannedDate,
        String summary,
        List<String> tags
) {
}
