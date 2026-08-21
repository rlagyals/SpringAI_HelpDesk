package com.skala.mealcard.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.mealcard.domain.MealRequest;
import com.skala.mealcard.service.MealRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/approvals")
@Tag(
        name = "Manager Approval",
        description = "MANAGER가 회식 신청을 승인하거나 반려합니다."
)
public class ApprovalController {

    private final MealRequestService service;

    public ApprovalController(MealRequestService service) {
        this.service = service;
    }

    @PostMapping("/{requestId}/approve")
    @Operation(
            summary = "회식 신청 승인",
            description = "같은 팀의 MANAGER만 승인할 수 있습니다."
    )
    public MealRequest approve(
            @PathVariable String requestId,
            @RequestParam String managerUserId) {

        return service.approveByManager(
                managerUserId,
                requestId
        );
    }

    @PostMapping("/{requestId}/reject")
    @Operation(
            summary = "회식 신청 반려",
            description = "같은 팀의 MANAGER만 반려할 수 있습니다."
    )
    public MealRequest reject(
            @PathVariable String requestId,
            @RequestParam String managerUserId) {

        return service.rejectByManager(
                managerUserId,
                requestId
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }
}