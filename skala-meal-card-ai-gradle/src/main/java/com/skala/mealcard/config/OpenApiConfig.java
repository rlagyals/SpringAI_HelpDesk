package com.skala.mealcard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI mealCardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SKALA 법인카드 회식 AI API")
                        .version("1.0.0")
                        .description("RAG + Tool + Memory + 권한 검증 + 승인 API + SSE 종합 실습"));
    }
}
