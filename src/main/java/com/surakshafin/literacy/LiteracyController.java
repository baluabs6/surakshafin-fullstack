package com.surakshafin.literacy;

import com.surakshafin.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/literacy")
public class LiteracyController {

    private final LiteracyContentRepository repository;

    public LiteracyController(LiteracyContentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<Dtos.ContentView>> list(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(required = false) String topic) {
        List<LiteracyContent> content = (topic == null || topic.isBlank())
                ? repository.findByLanguage(language)
                : repository.findByTopicAndLanguage(topic, language);
        return ApiResponse.ok(content.stream().map(Dtos.ContentView::from).toList());
    }
}
