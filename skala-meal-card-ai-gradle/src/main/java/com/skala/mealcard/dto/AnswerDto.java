package com.skala.mealcard.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 답변")
public record AnswerDto(

        @Schema(description = "AI가 실제로 생성한 답변")
        String answer,

        List<SourceDto> sources

) {}