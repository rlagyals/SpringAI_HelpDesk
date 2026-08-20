package com.skala.mealcard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meal-card")
public record AppProperties(
        Rag rag,
        Memory memory,
        Chunk chunk
) {
    public record Rag(int topK, double threshold) {}
    public record Memory(int maxMessages) {}
    public record Chunk(int maxChars, int minChars) {}
}
