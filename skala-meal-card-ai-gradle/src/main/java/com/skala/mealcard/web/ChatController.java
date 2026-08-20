package com.skala.mealcard.web;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.skala.mealcard.chat.MealCardService;
import com.skala.mealcard.dto.AnswerDto;
import com.skala.mealcard.dto.AskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    @Operation(summary = "SSE 응답 확인", description = "Phase 6 검증용 SSE API입니다.")
    public SseEmitter stream(@Valid @RequestBody AskRequest req) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                AnswerDto answer = service.ask(req);

                for (String token : answer.answer().split("(?<=\\s)")) {
                    if (!token.isBlank()) {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    }
                }

                emitter.send(SseEmitter.event().name("sources").data(answer.sources()));
                emitter.send(SseEmitter.event().name("done").data("done"));
                emitter.complete();
            }
            catch (IOException e) {
                emitter.completeWithError(e);
            }
            catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
