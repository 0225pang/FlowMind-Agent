package com.flowmind.content.service;

import com.flowmind.content.dto.CopyDraftUpdateRequest;
import com.flowmind.content.dto.CopyImageCreateRequest;
import com.flowmind.content.entity.ContentCopyEntity;
import com.flowmind.content.entity.ContentImageEntity;
import com.flowmind.content.entity.ContentThemeEntity;
import com.flowmind.content.mapper.ContentMapper;
import com.flowmind.content.vo.ContentCalendarItemVO;
import com.flowmind.content.vo.ContentThemeVO;
import com.flowmind.content.vo.CopyDraftVO;
import com.flowmind.content.vo.CopyImageVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ContentLibraryService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContentMapper contentMapper;

    public ContentLibraryService(ContentMapper contentMapper) {
        this.contentMapper = contentMapper;
    }

    public List<ContentThemeVO> listThemes(String keyword, String status, String channel) {
        return contentMapper.findThemes(keyword, status, channel).stream()
                .map(this::toThemeVO)
                .toList();
    }

    public List<CopyDraftVO> listDrafts(String keyword, String channel, String usageStatus) {
        return contentMapper.findCopies(keyword, channel, usageStatus).stream()
                .map(this::toDraftVO)
                .toList();
    }

    public List<CopyDraftVO> listDraftsByTheme(Long themeId) {
        return contentMapper.findCopiesByThemeId(themeId).stream()
                .map(this::toDraftVO)
                .toList();
    }

    public List<ContentCalendarItemVO> listCalendar(String month) {
        return contentMapper.findCalendar(month).stream()
                .map(row -> new ContentCalendarItemVO(
                        row.id(),
                        row.copyId(),
                        row.themeId(),
                        row.publishDate().toString(),
                        row.title(),
                        row.channel(),
                        row.publishStatus(),
                        row.usageStatus()
                ))
                .toList();
    }

    public CopyDraftVO updateDraft(Long id, CopyDraftUpdateRequest request) {
        contentMapper.updateCopy(id, request);
        ContentCopyEntity copy = contentMapper.findCopyById(id)
                .orElseThrow(() -> new IllegalArgumentException("文案不存在：" + id));
        return toDraftVO(copy);
    }

    public CopyImageVO addImage(Long copyId, CopyImageCreateRequest request) {
        ContentImageEntity image = contentMapper.insertImage(copyId, request);
        return toImageVO(image);
    }

    public void updateThemeStatus(Long id, String status) {
        contentMapper.updateThemeStatus(id, status);
    }

    public List<ContentThemeVO> generateTopics(String theme, String style) {
        String safeTheme = hasText(theme) ? theme : "保研规划";
        String safeStyle = hasText(style) ? style : "干货";
        for (int i = 1; i <= 10; i++) {
            contentMapper.insertGeneratedTheme(
                    safeTheme + "选题 " + i,
                    "小红书",
                    safeStyle,
                    "待创作",
                    70 + i
            );
        }
        return listThemes(safeTheme, null, "小红书");
    }

    public ContentThemeVO createTheme(String title, String platform, String type, String status,
                                      Integer heat, String plannedDate, String summary, String topic,
                                      List<String> tags) {
        Long id = contentMapper.insertTheme(
                fallback(title, "未命名主题"),
                fallback(platform, "小红书"),
                fallback(type, "经验干货"),
                fallback(status, "待创作"),
                heat == null ? 0 : heat,
                plannedDate,
                fallback(summary, "手动创建的内容主题，等待补充文案和排期。"),
                fallback(topic, title),
                tags
        );
        ContentThemeEntity theme = contentMapper.findThemeById(id)
                .orElseThrow(() -> new IllegalArgumentException("主题创建失败：" + id));
        return toThemeVO(theme);
    }

    public void deleteTheme(Long id) {
        contentMapper.softDeleteTheme(id);
    }

    public CopyDraftVO createCopy(Long themeId, String title, String channel, String version,
                                  String style, String content, String owner) {
        Long id = contentMapper.insertCopy(
                themeId,
                fallback(title, "未命名文案"),
                fallback(channel, "小红书"),
                fallback(version, "干货版"),
                fallback(style, "干货"),
                fallback(content, "请补充文案正文。"),
                fallback(owner, "内容运营")
        );
        ContentCopyEntity copy = contentMapper.findCopyById(id)
                .orElseThrow(() -> new IllegalArgumentException("文案创建失败：" + id));
        return toDraftVO(copy);
    }

    public void deleteCopy(Long id) {
        contentMapper.softDeleteCopy(id);
    }

    public ContentThemeVO rateTheme(Long id, Integer rating) {
        validateRating(rating);
        contentMapper.updateThemeRating(id, rating);
        ContentThemeEntity theme = contentMapper.findThemeById(id)
                .orElseThrow(() -> new IllegalArgumentException("主题不存在：" + id));
        return toThemeVO(theme);
    }

    public CopyDraftVO rateCopy(Long id, Integer rating) {
        validateRating(rating);
        contentMapper.updateCopyRating(id, rating);
        ContentCopyEntity copy = contentMapper.findCopyById(id)
                .orElseThrow(() -> new IllegalArgumentException("文案不存在：" + id));
        return toDraftVO(copy);
    }

    private ContentThemeVO toThemeVO(ContentThemeEntity theme) {
        return new ContentThemeVO(
                theme.getId(),
                theme.getTitle(),
                theme.getTopic(),
                theme.getPlatform(),
                theme.getType(),
                theme.getStatus(),
                theme.getHeat(),
                theme.getRating(),
                contentMapper.findTagsByThemeId(theme.getId()),
                theme.getPlannedDate() == null ? null : theme.getPlannedDate().toString(),
                theme.getSummary(),
                listDraftsByTheme(theme.getId())
        );
    }

    private CopyDraftVO toDraftVO(ContentCopyEntity copy) {
        return new CopyDraftVO(
                copy.getId(),
                copy.getThemeId(),
                copy.getTitle(),
                copy.getChannel(),
                copy.getVersion(),
                copy.getStyle(),
                copy.getContent(),
                copy.getUsageStatus(),
                copy.getUsedDate() == null ? null : copy.getUsedDate().toString(),
                copy.getGeneratedAt() == null ? null : copy.getGeneratedAt().format(DATE_TIME_FORMATTER),
                copy.getOwner(),
                copy.getFeedback(),
                copy.getRating(),
                contentMapper.findImagesByCopyId(copy.getId()).stream().map(this::toImageVO).toList(),
                copy.getImageSuggestion()
        );
    }

    private CopyImageVO toImageVO(ContentImageEntity image) {
        return new CopyImageVO(image.getId(), image.getFileName(), image.getUrl());
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在 1-5 之间");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String fallback(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }
}
