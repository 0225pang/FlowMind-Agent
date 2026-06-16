package com.flowmind.content.vo;

public record ContentCalendarItemVO(
        Long id,
        Long draftId,
        Long themeId,
        String date,
        String title,
        String channel,
        String status,
        String usageStatus
) {
}
