package com.skala.mealcard.chat;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.mealcard.dto.AnswerDto;
import com.skala.mealcard.dto.AskRequest;
import com.skala.mealcard.dto.SourceDto;
import com.skala.mealcard.tools.ToolUsageContext;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MealCardService {

    private static final Logger log = LoggerFactory.getLogger(MealCardService.class);

    private final ChatClient chat;
    private final ChatMemory memory;
    private final ObjectMapper objectMapper;

    public MealCardService(
            @Qualifier("mealCardChatClient") ChatClient chat,
            ChatMemory memory,
            ObjectMapper objectMapper) {
        this.chat = chat;
        this.memory = memory;
        this.objectMapper = objectMapper;
    }

    public AnswerDto ask(AskRequest req) {
        String conversationId = conversationId(req.userId(), req.sessionId());
        AtomicBoolean toolUsed = new AtomicBoolean(false);

        Map<String, Object> toolContext = toolContext(req.userId());
        toolContext.put(ToolUsageContext.FLAG_KEY, toolUsed);

        ChatClientResponse response = chat.prompt()
                .user(req.question())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(toolContext)
                .call()
                .chatClientResponse();

        if (response == null || response.chatResponse() == null) {
            return new AnswerDto("응답을 생성하지 못했습니다.", List.of(), false);
        }

        String answer = response.chatResponse()
                .getResult()
                .getOutput()
                .getText();

        try {
            var usage = response.chatResponse().getMetadata().getUsage();
            log.info("token usage: prompt={}, completion={}, total={}",
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }
        catch (Exception ignored) {
            log.debug("토큰 사용량 메타데이터를 읽을 수 없습니다.");
        }

        return new AnswerDto(answer, extractSources(response), toolUsed.get());
    }

    /**
     * 긴 답변의 첫 글자를 빨리 보여주기 위해 모델 토큰을 그대로 SSE로 흘려보낸다.
     * 토큰 스트림이 끝나면 마지막 이벤트로 출처(sources)를 한 번 더 내보낸다.
     */
    public Flux<ServerSentEvent<String>> stream(AskRequest req) {
        String conversationId = conversationId(req.userId(), req.sessionId());
        AtomicReference<ChatClientResponse> lastResponse = new AtomicReference<>();

        Flux<ServerSentEvent<String>> tokens = chat.prompt()
                .user(req.question())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(toolContext(req.userId()))
                .stream()
                .chatClientResponse()
                .doOnNext(lastResponse::set)
                .map(this::deltaText)
                .filter(text -> !text.isEmpty())
                .map(text -> ServerSentEvent.<String>builder(text).event("token").build());

        Mono<ServerSentEvent<String>> sourcesEvent = Mono.fromCallable(() -> {
            List<SourceDto> sources = lastResponse.get() == null
                    ? List.of()
                    : extractSources(lastResponse.get());

            return ServerSentEvent.<String>builder(writeJson(sources)).event("sources").build();
        });

        return tokens.concatWith(sourcesEvent);
    }

    private String deltaText(ChatClientResponse response) {
        if (response.chatResponse() == null || response.chatResponse().getResult() == null) {
            return "";
        }

        String text = response.chatResponse().getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception e) {
            log.warn("SSE sources 직렬화 실패", e);
            return "[]";
        }
    }

    private Map<String, Object> toolContext(String userId) {
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        return context;
    }

    public List<Map<String, String>> history(String userId, String sessionId) {
        String conversationId = conversationId(userId, sessionId);

        return memory.get(conversationId).stream()
                .map(this::toRow)
                .toList();
    }

    private Map<String, String> toRow(Message message) {
        return Map.of(
                "role", message.getMessageType().getValue(),
                "text", message.getText()
        );
    }

    private String conversationId(String userId, String sessionId) {
        return "default:%s:%s".formatted(userId, sessionId);
    }

    private List<SourceDto> extractSources(ChatClientResponse response) {
        Object raw = response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (!(raw instanceof List<?> docs)) {
            return List.of();
        }

        Set<SourceDto> result = new LinkedHashSet<>();

        for (Object item : docs) {
            if (item instanceof Document doc) {
                result.add(new SourceDto(
                        String.valueOf(doc.getMetadata().getOrDefault("source", "unknown")),
                        String.valueOf(doc.getMetadata().getOrDefault("version", "unknown"))
                ));
            }
        }

        return List.copyOf(result);
    }
}
