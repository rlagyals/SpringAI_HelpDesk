package com.skala.mealcard.tools;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.mealcard.domain.MealRequest;
import com.skala.mealcard.domain.MealType;
import com.skala.mealcard.repository.MealRequestRepository;
import com.skala.mealcard.service.MealRequestService;

@Component
public class MealRequestTools {

    private final MealRequestService service;
    private final MealRequestRepository requests;

    public MealRequestTools(MealRequestService service, MealRequestRepository requests) {
        this.service = service;
        this.requests = requests;
    }

    @Tool(description = "법인카드 회식을 위해 사전 신청을 생성한다. AI는 신청만 생성하며 승인하지 않는다.")
    public Map<String, Object> createMealRequest(
            @ToolParam(description = "회식 예정일. YYYY-MM-DD 형식") String mealDate,
            @ToolParam(description = "참석 인원 수") int attendeeCount,
            @ToolParam(description = "회식 유형. REGULAR, WELCOME, PROJECT_END 중 하나") String mealType,
            @ToolParam(description = "회식 사유") String reason,
            @ToolParam(description = "오후 10시 이후 결제 예정 여부") boolean lateNightPlanned,
            @ToolParam(description = "주류 포함 예정 여부") boolean alcoholPlanned,
            @ToolParam(description = "심야 또는 주류 예정 시 사유. 해당 없으면 빈 문자열") String specialReason,
            ToolContext context) {

        try {
            String userId = String.valueOf(context.getContext().get("userId"));
            MealType type = MealType.valueOf(mealType.trim().toUpperCase());

            MealRequest created = service.createRequest(
                    userId,
                    LocalDate.parse(mealDate),
                    attendeeCount,
                    type,
                    reason,
                    lateNightPlanned,
                    alcoholPlanned,
                    specialReason
            );

            return toMap(created);
        }
        catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @Tool(description = "현재 사용자가 생성한 회식 사전 신청의 상태와 승인번호를 조회한다. requestId가 없으면 가장 최근 신청을 조회할 수 있다.")
    public Map<String, Object> getMealRequestStatus(
            @ToolParam(description = "회식 신청 ID. 예: MR-1234ABCD. 최근 신청을 조회하려면 LATEST") String requestId,
            ToolContext context) {

        try {
            String userId = String.valueOf(context.getContext().get("userId"));

            MealRequest request;
            if (requestId == null || requestId.isBlank() || "LATEST".equalsIgnoreCase(requestId)) {
                request = requests.findLatestByApplicant(userId)
                        .orElseThrow(() -> new IllegalArgumentException("최근 신청 내역이 없습니다."));
            }
            else {
                request = requests.findById(requestId)
                        .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));

                if (!request.applicantUserId().equals(userId)) {
                    throw new IllegalStateException("본인이 생성한 신청만 조회할 수 있습니다.");
                }
            }

            return toMap(request);
        }
        catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Map<String, Object> toMap(MealRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", request.requestId());
        result.put("teamId", request.teamId());
        result.put("mealDate", request.mealDate().toString());
        result.put("attendeeCount", request.attendeeCount());
        result.put("mealType", request.mealType().name());
        result.put("mealTypeLabel", request.mealType().label());
        result.put("expectedAmount", request.expectedAmount());
        result.put("lateNightPlanned", request.lateNightPlanned());
        result.put("alcoholPlanned", request.alcoholPlanned());
        result.put("status", request.status().name());
        result.put("approvalNumber", request.approvalNumber() == null ? "" : request.approvalNumber());
        return result;
    }
}
