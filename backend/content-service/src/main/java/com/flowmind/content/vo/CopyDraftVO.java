package com.flowmind.content.vo;

import java.util.List;

public record CopyDraftVO(
        Long id,
        Long themeId,
        String title,
        String channel,
        String version,
        String style,
        String content,
        String usageStatus,
        String usedDate,
        String generatedAt,
        String owner,
        String feedback,
        Integer rating,
        List<CopyImageVO> images,
        String imageSuggestion
) {
}
