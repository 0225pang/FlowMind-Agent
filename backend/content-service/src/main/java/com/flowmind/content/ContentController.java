package com.flowmind.content;

import com.flowmind.common.core.ApiResponse;
import com.flowmind.content.dto.ContentGenerateRequest;
import com.flowmind.content.dto.ContentStatusUpdateRequest;
import com.flowmind.content.dto.CopyCreateRequest;
import com.flowmind.content.dto.CopyDraftUpdateRequest;
import com.flowmind.content.dto.CopyImageCreateRequest;
import com.flowmind.content.dto.RatingRequest;
import com.flowmind.content.dto.ThemeCreateRequest;
import com.flowmind.content.service.ContentLibraryService;
import com.flowmind.content.service.ContentSopService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {
    private final ContentSopService sopService;
    private final ContentLibraryService libraryService;

    public ContentController(ContentSopService sopService, ContentLibraryService libraryService) {
        this.sopService = sopService;
        this.libraryService = libraryService;
    }

    @GetMapping("/themes")
    public ApiResponse<?> themes(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "channel", required = false) String channel
    ) {
        return ApiResponse.success(libraryService.listThemes(keyword, status, channel));
    }

    @PostMapping("/themes")
    public ApiResponse<?> createTheme(@RequestBody ThemeCreateRequest request) {
        return ApiResponse.success(libraryService.createTheme(
                request.title(),
                request.platform(),
                request.type(),
                request.status(),
                request.heat(),
                request.plannedDate(),
                request.summary(),
                request.topic(),
                request.tags()
        ));
    }

    @DeleteMapping("/themes/{id}")
    public ApiResponse<?> deleteTheme(@PathVariable("id") Long id) {
        libraryService.deleteTheme(id);
        return ApiResponse.success();
    }

    @PutMapping("/themes/{id}/rating")
    public ApiResponse<?> rateTheme(@PathVariable("id") Long id, @RequestBody RatingRequest request) {
        return ApiResponse.success(libraryService.rateTheme(id, request.rating()));
    }

    @GetMapping("/themes/{themeId}/drafts")
    public ApiResponse<?> themeDrafts(@PathVariable("themeId") Long themeId) {
        return ApiResponse.success(libraryService.listDraftsByTheme(themeId));
    }

    @PostMapping("/themes/{themeId}/drafts")
    public ApiResponse<?> createCopy(@PathVariable("themeId") Long themeId, @RequestBody CopyCreateRequest request) {
        return ApiResponse.success(libraryService.createCopy(
                themeId,
                request.title(),
                request.channel(),
                request.version(),
                request.style(),
                request.content(),
                request.owner()
        ));
    }

    @GetMapping("/drafts")
    public ApiResponse<?> drafts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "channel", required = false) String channel,
            @RequestParam(name = "usageStatus", required = false) String usageStatus
    ) {
        return ApiResponse.success(libraryService.listDrafts(keyword, channel, usageStatus));
    }

    @PutMapping("/drafts/{draftId}")
    public ApiResponse<?> updateDraft(@PathVariable("draftId") Long draftId, @RequestBody CopyDraftUpdateRequest request) {
        return ApiResponse.success(libraryService.updateDraft(draftId, request));
    }

    @DeleteMapping("/drafts/{draftId}")
    public ApiResponse<?> deleteCopy(@PathVariable("draftId") Long draftId) {
        libraryService.deleteCopy(draftId);
        return ApiResponse.success();
    }

    @PutMapping("/drafts/{id}/rating")
    public ApiResponse<?> rateCopy(@PathVariable("id") Long id, @RequestBody RatingRequest request) {
        return ApiResponse.success(libraryService.rateCopy(id, request.rating()));
    }

    @PostMapping("/drafts/{draftId}/images")
    public ApiResponse<?> addDraftImage(@PathVariable("draftId") Long draftId, @RequestBody CopyImageCreateRequest request) {
        return ApiResponse.success(libraryService.addImage(draftId, request));
    }

    @GetMapping("/calendar")
    public ApiResponse<?> calendar(@RequestParam(name = "month", required = false) String month) {
        return ApiResponse.success(libraryService.listCalendar(month));
    }

    @GetMapping("/topics")
    public ApiResponse<?> topics() {
        return ApiResponse.success(libraryService.listThemes(null, null, null));
    }

    @PostMapping("/topics/generate")
    public ApiResponse<?> generateTopics(@RequestBody Map<String, Object> body) {
        String theme = String.valueOf(body.getOrDefault("theme", "保研规划"));
        String style = String.valueOf(body.getOrDefault("style", "干货"));
        return ApiResponse.success(libraryService.generateTopics(theme, style));
    }

    @PostMapping("/moments/generate")
    public ApiResponse<?> moments(@RequestBody Map<String, Object> body) {
        String scene = String.valueOf(body.getOrDefault("scene", "带学员复盘申请节奏"));
        return ApiResponse.success(List.of(
                scene + "的时候，我更在意的不是把结果讲得多漂亮，而是把每一步专业动作讲清楚：材料怎么定位、风险怎么提前发现、面试怎么复盘。",
                "今天和同学复盘材料，重点不是简单改简历，而是把每段经历背后的能力证据讲清楚。很多申请结果，都是这些细节一点点累积出来的。"
        ));
    }

    @PostMapping("/articles/generate")
    public ApiResponse<?> articles(@RequestBody Map<String, Object> body) {
        String topic = String.valueOf(body.getOrDefault("topic", "保研规划"));
        return ApiResponse.success(List.of(
                topic + "：从目标拆解到材料准备的完整路线",
                "从 GPA 到科研：普通院校学生的保研补强路径",
                "一篇讲清楚夏令营、预推免和九推的时间差"
        ));
    }

    @PutMapping("/topics/{id}/status")
    public ApiResponse<?> status(@PathVariable("id") Long id, @RequestBody ContentStatusUpdateRequest request) {
        libraryService.updateThemeStatus(id, request.status());
        return ApiResponse.success();
    }

    @PostMapping("/sop/xiaohongshu/generate")
    public ApiResponse<?> generateXiaohongshu(@RequestBody ContentGenerateRequest request) {
        return ApiResponse.success(sopService.generateXiaohongshu(request));
    }

    @PostMapping("/sop/moments/generate")
    public ApiResponse<?> generateMoments(@RequestBody ContentGenerateRequest request) {
        return ApiResponse.success(sopService.generateMoments(request));
    }

    @PostMapping("/sop/assets/extract")
    public ApiResponse<?> extractAssets(@RequestBody ContentGenerateRequest request) {
        return ApiResponse.success(sopService.extractAsset(request));
    }

    @GetMapping("/sop/architecture")
    public ApiResponse<?> architecture() {
        return ApiResponse.success(sopService.architecture());
    }
}
