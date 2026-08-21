package com.skala.mealcard.web;

import java.util.List;
import java.util.Map;

import com.skala.mealcard.chat.MealCardService;
import com.skala.mealcard.dto.AnswerDto;
import com.skala.mealcard.dto.AskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@Tag(name = "AI Chat", description = "Swagger에서 질문을 입력하고 바로 답변을 확인하는 API")
public class ChatController {

    private final MealCardService service;

    public ChatController(MealCardService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    @Operation(
        summary = "AI에게 질문하기",
        description = "질문을 입력하면 RAG / Tool / Memory를 이용해 답변합니다."
)
public AnswerDto chat(
        @org.springframework.web.bind.annotation.RequestParam String q,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "member-a1") String userId,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "s1") String sessionId) {

    AskRequest req = new AskRequest(q, userId, sessionId);

    return service.ask(req);
}

    @GetMapping("/chat/history")
    @Operation(
        summary = "대화 이력 조회",
        description = "userId와 sessionId로 저장된 대화 이력(memory)을 조회합니다."
    )
    public List<Map<String, String>> history(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "member-a1") String userId) {

        return service.history(userId, sessionId);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "AI에게 질문하기 (SSE 스트리밍)",
        description = "긴 답변의 첫 글자를 빨리 보여주기 위해 토큰 단위로 스트리밍하고, "
                + "마지막에 출처(sources)를 별도 이벤트로 내보냅니다."
    )
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody AskRequest req) {
        return service.stream(req);
    }
}
