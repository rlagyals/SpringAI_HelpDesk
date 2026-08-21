package com.skala.mealcard.web;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skala.mealcard.domain.MealRequest;
import com.skala.mealcard.repository.MealRequestRepository;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "RAG Admin", description = "인제스트된 RAG 청크 품질 확인")
public class AdminController {

    private final VectorStore vectorStore;
    private final MealRequestRepository mealRequests;

    public AdminController(VectorStore vectorStore, MealRequestRepository mealRequests) {
        this.vectorStore = vectorStore;
        this.mealRequests = mealRequests;
    }

    @GetMapping("/meal-requests")
    @Operation(
        summary = "메모리에 남아있는 회식 신청(티켓) 전체 조회",
        description = "X-Role 헤더에 ADMIN을 입력합니다. 서버 재시작 시 초기화되는 인메모리 저장소 기준입니다."
    )
    public List<MealRequest> mealRequests(
            @Parameter(example = "ADMIN")
            @RequestHeader(name = "X-Role", defaultValue = "") String role) {

        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN 권한이 필요합니다.");
        }

        return mealRequests.findAll();
    }

    @GetMapping("/chunks")
    @Operation(summary = "검색되는 청크 확인", description = "X-Role 헤더에 ADMIN을 입력합니다.")
    public List<Map<String, Object>> inspect(
            @Parameter(example = "ADMIN")
            @RequestHeader(name = "X-Role", defaultValue = "") String role,

            @Parameter(description = "검색 질문", example = "주류 결제 규정")
            @RequestParam String q,

            @Parameter(example = "5")
            @RequestParam(defaultValue = "5") int topK) {

        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN 권한이 필요합니다.");
        }

        int safeTopK = Math.max(1, Math.min(topK, 20));

        return vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(q)
                                .topK(safeTopK)
                                .build())
                .stream()
                .map(doc -> Map.<String, Object>of(
                        "source", doc.getMetadata().getOrDefault("source", ""),
                        "version", doc.getMetadata().getOrDefault("version", ""),
                        "score", doc.getScore() == null ? 0.0 : doc.getScore(),
                        "preview", preview(doc.getText())
                ))
                .toList();
    }

    private String preview(String text) {
        if (text == null) return "";
        return text.substring(0, Math.min(160, text.length()));
    }
}
