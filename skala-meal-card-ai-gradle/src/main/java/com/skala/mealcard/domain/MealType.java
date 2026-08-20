package com.skala.mealcard.domain;

public enum MealType {
    REGULAR("평일 일반 회식", 40_000),
    WELCOME("신규 입사자 환영회", 50_000),
    PROJECT_END("프로젝트 종료 회식", 60_000);

    private final String label;
    private final int perPersonLimit;

    MealType(String label, int perPersonLimit) {
        this.label = label;
        this.perPersonLimit = perPersonLimit;
    }

    public String label() {
        return label;
    }

    public int perPersonLimit() {
        return perPersonLimit;
    }
}
