package com.skala.mealcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RAG 출처")
public record SourceDto(
        @Schema(example = "meal-expense-policy.md") String source,
        @Schema(example = "2026-08") String version
) {}
