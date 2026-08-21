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
import com.skala.mealcard.service.MealRequestService;

@Component
public class MealRequestTools {

    private final MealRequestService service;

    public MealRequestTools(MealRequestService service) {
        this.service = service;
    }


    @Tool(description = """
        법인카드 회식을 위해 사전 신청을 생성한다.
        AI는 신청만 생성하며 승인하지 않는다.
        회식 예정일, 참석 인원 수, 회식 유형, 회식 사유가 반드시 필요하다.
        예상 사용 금액은 참석 인원 수와 회식 유형별 1인당 한도를 기준으로 서버에서 자동 계산한다.
        사용자에게 예상 사용 금액을 직접 입력하도록 요구하지 않는다.
        필수 정보가 없으면 임의로 추측하지 말고 사용자에게 다시 요청한다.
        """)
    public Map<String, Object> createMealRequest(
            @ToolParam(description = "회식 예정일. YYYY-MM-DD 형식")
            String mealDate,

            @ToolParam(description = "참석 인원 수")
            int attendeeCount,

            @ToolParam(description = "회식 유형. REGULAR, WELCOME, PROJECT_END 중 하나")
            String mealType,

            @ToolParam(description = "회식 사유")
            String reason,

            @ToolParam(description = "오후 10시 이후 결제 예정 여부")
            boolean lateNightPlanned,

            @ToolParam(description = "주류 포함 예정 여부")
            boolean alcoholPlanned,

            @ToolParam(description = "심야 또는 주류 예정 시 사유. 해당 없으면 빈 문자열")
            String specialReason,

            ToolContext context) {

        try {
            String userId =
                    String.valueOf(context.getContext().get("userId"));

            MealType type =
                    MealType.valueOf(mealType.trim().toUpperCase());

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
            return Map.of(
                    "error",
                    e.getMessage()
            );
        }
    }

    @Tool(description = """
            회식 사전 신청의 상태와 승인번호를 조회한다.
            반드시 사용자가 제공한 실제 접수번호(requestId)가 있어야 호출한다.
            접수번호가 없으면 이 도구를 호출하지 마세요.
            최근 신청을 임의로 조회하지 마세요.
            대화 내용만 보고 신청 상태를 추측하지 마세요.
            PENDING_MANAGER_APPROVAL, APPROVED 등의 상태는
            실제 접수번호로 조회된 결과가 있을 때만 답한다.
            """)
    public Map<String, Object> getMealRequestStatus(
            @ToolParam(description = "회식 신청 접수번호. 예: MR-1234ABCD")
            String requestId,
            ToolContext context) {

        try {
            String userId =
                    String.valueOf(context.getContext().get("userId"));

            if (requestId == null || requestId.isBlank()) {
                return Map.of(
                        "error",
                        "회식 신청 상태를 조회하려면 접수번호가 필요합니다. 접수번호(MR-...)를 알려주세요."
                );
            }

            MealRequest request =
                    service.getRequestStatus(requestId, userId);

            return toMap(request);
        }
        catch (Exception e) {
            return Map.of(
                    "error",
                    e.getMessage()
            );
        }
    }

    private Map<String, Object> toMap(MealRequest request) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "requestId",
                request.requestId()
        );

        result.put(
                "teamId",
                request.teamId()
        );

        result.put(
                "mealDate",
                request.mealDate().toString()
        );

        result.put(
                "attendeeCount",
                request.attendeeCount()
        );

        result.put(
                "mealType",
                request.mealType().name()
        );

        result.put(
                "mealTypeLabel",
                request.mealType().label()
        );

        result.put(
                "expectedAmount",
                request.expectedAmount()
        );

        result.put(
                "lateNightPlanned",
                request.lateNightPlanned()
        );

        result.put(
                "alcoholPlanned",
                request.alcoholPlanned()
        );

        result.put(
                "status",
                request.status().name()
        );

        result.put(
                "approvalNumber",
                request.approvalNumber() == null
                        ? ""
                        : request.approvalNumber()
        );

        return result;
    }
}