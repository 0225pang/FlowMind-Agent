package com.flowmind.content.dto;

import java.time.LocalDate;

public record CopyDraftUpdateRequest(
        String title,
        String content,
        String usageStatus,
        LocalDate usedDate,
        String feedback,
        String imageSuggestion
) {
}
