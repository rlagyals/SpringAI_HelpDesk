package com.skala.mealcard.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.skala.mealcard.tools.MealRequestTools;
import com.skala.mealcard.tools.TeamBudgetTools;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AiConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    ChatMemory chatMemory(AppProperties props) {
        return MessageWindowChatMemory.builder()
                .maxMessages(props.memory().maxMessages())
                .build();
    }

    @Bean
    @Qualifier("mealCardChatClient")
    ChatClient mealCardChatClient(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            ChatMemory chatMemory,
            AppProperties props,
            TeamBudgetTools teamBudgetTools,
            MealRequestTools mealRequestTools,
            @org.springframework.beans.factory.annotation.Value("classpath:prompts/system.st")
            Resource systemPrompt) {

        SearchRequest searchRequest = SearchRequest.builder()
                .topK(props.rag().topK())
                .similarityThreshold(props.rag().threshold())
                .build();

        return builder
                .defaultSystem(systemPrompt)
                .defaultTools(teamBudgetTools, mealRequestTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(searchRequest)
                                .build(),
                        SafeGuardAdvisor.builder()
                                .sensitiveWords(List.of(
                                        "OPENAI_API_KEY",
                                        "비밀번호 알려줘",
                                        "password 알려줘"))
                                .failureResponse("민감정보 관련 요청은 처리할 수 없습니다.")
                                .build(),
                        SimpleLoggerAdvisor.builder().build()
                )
                .build();
    }
}
