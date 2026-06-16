package com.flowmind.content.vo;

import java.util.List;

public record ContentThemeVO(
        Long id,
        String title,
        String topic,
        String platform,
        String type,
        String status,
        Integer heat,
        Integer rating,
        List<String> tags,
        String plannedDate,
        String summary,
        List<CopyDraftVO> drafts
) {
}
