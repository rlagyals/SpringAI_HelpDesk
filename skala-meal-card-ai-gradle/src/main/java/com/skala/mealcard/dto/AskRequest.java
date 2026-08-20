package com.skala.mealcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI 질문 요청")
public record AskRequest(

        @Schema(description = "질문")
        @NotBlank String question,

        @Schema(description = "데모 사용자 ID")
        @NotBlank String userId,

        @Schema(description = "대화 세션 ID. 같은 값을 유지하면 이전 대화를 기억함")
        @NotBlank String sessionId

) {}