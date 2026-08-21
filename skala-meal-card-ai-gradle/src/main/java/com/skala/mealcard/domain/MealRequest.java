package com.skala.mealcard.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MealRequest(
        String requestId,
        String teamId,
        String applicantUserId,
        LocalDate mealDate,
        int attendeeCount,
        MealType mealType,
        String reason,
        int expectedAmount,
        boolean lateNightPlanned,
        boolean alcoholPlanned,
        String specialReason,
        RequestStatus status,
        String approvalNumber,
        LocalDateTime createdAt,
        LocalDateTime approvedAt
) {
    public MealRequest withStatus(RequestStatus newStatus, String newApprovalNumber, LocalDateTime newApprovedAt) {
        return new MealRequest(
                requestId, teamId, applicantUserId, mealDate, attendeeCount, mealType,
                reason, expectedAmount, lateNightPlanned, alcoholPlanned, specialReason,
                newStatus, newApprovalNumber, createdAt, newApprovedAt
        );
    }

    public MealRequest withDetails(
            LocalDate newMealDate,
            int newAttendeeCount,
            MealType newMealType,
            String newReason,
            int newExpectedAmount,
            boolean newLateNightPlanned,
            boolean newAlcoholPlanned,
            String newSpecialReason,
            RequestStatus newStatus) {
        return new MealRequest(
                requestId, teamId, applicantUserId, newMealDate, newAttendeeCount, newMealType,
                newReason, newExpectedAmount, newLateNightPlanned, newAlcoholPlanned, newSpecialReason,
                newStatus, approvalNumber, createdAt, approvedAt
        );
    }
}
