package com.skala.mealcard.domain;

public record Team(
        String teamId,
        String teamName,
        int memberCount,
        int quarterlyBudget,
        int usedBudget
) {
    public int remainingBudget() {
        return quarterlyBudget - usedBudget;
    }
}
