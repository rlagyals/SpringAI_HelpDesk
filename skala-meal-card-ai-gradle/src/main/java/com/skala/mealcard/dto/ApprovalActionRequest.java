package com.skala.mealcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "MANAGER 승인/반려 요청")
public record ApprovalActionRequest(
        @Schema(description = "승인 또는 반려를 수행하는 매니저 ID", example = "manager-a")
        @NotBlank String managerUserId
) {}
