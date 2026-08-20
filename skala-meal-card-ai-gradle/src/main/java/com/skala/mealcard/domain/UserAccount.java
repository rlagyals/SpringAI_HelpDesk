package com.skala.mealcard.domain;

public record UserAccount(
        String userId,
        String name,
        String teamId,
        UserRole role
) {}
