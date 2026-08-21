package com.skala.mealcard.chat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.skala.mealcard.dto.AnswerDto;
import com.skala.mealcard.dto.AskRequest;
import com.skala.mealcard.dto.SourceDto;

@Service
public class MealCardService {

    private static final Logger log = LoggerFactory.getLogger(MealCardService.class);

    private final ChatClient chat;

    public MealCardService(@Qualifier("mealCardChatClient") ChatClient chat) {
        this.chat = chat;
    }

    public AnswerDto ask(AskRequest req) {
        String conversationId = "default:%s:%s".formatted(req.userId(), req.sessionId());

        ChatClientResponse response = chat.prompt()
                .user(req.question())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("userId", req.userId()))
                .call()
                .chatClientResponse();

        if (response == null || response.chatResponse() == null) {
            return new AnswerDto("응답을 생성하지 못했습니다.", List.of());
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

        return new AnswerDto(answer, extractSources(response));
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
