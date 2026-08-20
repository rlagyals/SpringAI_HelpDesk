package com.skala.mealcard.web;

import java.util.Map;

import com.skala.mealcard.domain.MealRequest;
import com.skala.mealcard.dto.ApprovalActionRequest;
import com.skala.mealcard.service.MealRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Manager Approval", description = "AI가 아닌 실제 MANAGER 사용자가 별도 API에서 승인/반려")
public class ApprovalController {

    private final MealRequestService service;

    public ApprovalController(MealRequestService service) {
        this.service = service;
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "회식 신청 승인", description = "같은 팀 MANAGER만 승인할 수 있습니다.")
    public Map<String, Object> approve(
            @Parameter(example = "MR-1234ABCD") @PathVariable String requestId,
            @Valid @RequestBody ApprovalActionRequest req) {

        MealRequest result = service.approveByManager(req.managerUserId(), requestId);

        return Map.of(
                "requestId", result.requestId(),
                "status", result.status().name(),
                "approvalNumber", result.approvalNumber() == null ? "" : result.approvalNumber()
        );
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "회식 신청 반려", description = "같은 팀 MANAGER만 반려할 수 있습니다.")
    public Map<String, Object> reject(
            @Parameter(example = "MR-1234ABCD") @PathVariable String requestId,
            @Valid @RequestBody ApprovalActionRequest req) {

        MealRequest result = service.rejectByManager(req.managerUserId(), requestId);

        return Map.of(
                "requestId", result.requestId(),
                "status", result.status().name()
        );
    }
}
