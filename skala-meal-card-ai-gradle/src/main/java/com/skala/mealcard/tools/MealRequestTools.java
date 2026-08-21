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
            법인카드 회식을 위한 사전 신청을 즉시 접수하고 접수번호(MR-...)를 발급한다.
            AI는 신청만 생성하며 승인하지 않는다.

            사용자가 회식 신청 의사를 밝히면 세부 항목을 먼저 캐묻지 않고 바로 호출한다.
            세부 항목(회식 예정일, 참석 인원 수, 회식 유형, 회식 사유,
            심야·주류 예정 여부)은 이 Tool의 인자가 아니며,
            접수 이후 submitMealRequestDetails Tool로 별도 제출한다.

            반환된 requestId는 사전 신청서 제출과 상태 조회에 사용되는
            접수번호이므로 반드시 사용자에게 그대로 안내한다.
            """)
    public Map<String, Object> createMealRequest(
            ToolContext context) {

        ToolUsageContext.markUsed(context);

        try {

            String userId =
                    String.valueOf(context.getContext().get("userId"));

            MealRequest created = service.startRequest(userId);

            return toMap(created);
        }
        catch (Exception e) {

            return Map.of(
                    "success", false,
                    "errorCode", normalizeCreateError(e)
            );
        }
    }

    @Tool(description = """
            이미 접수번호가 발급된 회식 신청에 사전 신청서(세부 항목)를 제출한다.

            createMealRequest로 발급받은 실제 접수번호(requestId)가 있을 때만 호출하며,
            접수번호를 임의로 만들어내지 않는다.

            회식 예정일, 참석 인원 수, 회식 유형, 회식 사유,
            심야 예정 여부, 주류 예정 여부가 필요하다.

            예상 사용 금액은 사용자가 입력하지 않으며
            서버에서 참석 인원 수와 회식 유형별 한도를 이용해 자동 계산한다.

            필요한 정보가 부족하면 임의로 추측하여 Tool을 호출하지 않는다.
            """)
    public Map<String, Object> submitMealRequestDetails(
            @ToolParam(description = "createMealRequest로 발급된 접수번호. 예: MR-1234ABCD")
            String requestId,

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

            @ToolParam(description = "심야 또는 주류 예정 시 관련 사유. 해당 없으면 빈 문자열")
            String specialReason,

            ToolContext context) {

        ToolUsageContext.markUsed(context);

        try {

            String userId =
                    String.valueOf(context.getContext().get("userId"));

            MealType type =
                    MealType.valueOf(mealType.trim().toUpperCase());

            MealRequest updated = service.submitDetails(
                    userId,
                    requestId,
                    LocalDate.parse(mealDate),
                    attendeeCount,
                    type,
                    reason,
                    lateNightPlanned,
                    alcoholPlanned,
                    specialReason
            );

            return toMap(updated);
        }
        catch (Exception e) {

            return Map.of(
                    "success", false,
                    "errorCode", normalizeSubmitError(e)
            );
        }
    }


    @Tool(description = """
            사용자가 제공한 접수번호로 회식 신청의 실제 상태와 승인번호를 조회한다.

            상태를 추측해서는 안 되며 반드시 이 Tool의 실제 결과를 사용한다.

            접수번호가 제공되지 않았다면 Tool을 호출하지 않는다.

            사용자가 MR-로 시작하는 값을 제공했다면
            길이나 형식만 보고 유효하지 않다고 판단하지 말고
            실제 존재 여부를 확인하기 위해 그 값을 그대로 조회한다.

            다른 팀의 신청 정보는 제공하지 않는다.
            """)
    public Map<String, Object> getMealRequestStatus(
            @ToolParam(description = """
                    사용자가 입력한 회식 신청 접수번호.
                    MR-로 시작하는 값이 있으면 형식이 완벽하지 않아도
                    실제 존재 여부를 확인하기 위해 그대로 전달한다.
                    예: MR-1234ABCD
                    """)
            String requestId,

            ToolContext context) {

        ToolUsageContext.markUsed(context);

        try {

            String userId =
                    String.valueOf(context.getContext().get("userId"));

            if (requestId == null || requestId.isBlank()) {
                return Map.of(
                        "success", false,
                        "errorCode", "REQUEST_ID_REQUIRED"
                );
            }

            MealRequest request =
                    service.getRequestStatus(requestId, userId);

            Map<String, Object> result = toMap(request);
            result.put("success", true);

            return result;
        }
        catch (IllegalArgumentException e) {

            return Map.of(
                    "success", false,
                    "errorCode", e.getMessage()
            );
        }
        catch (IllegalStateException e) {

            return Map.of(
                    "success", false,
                    "errorCode", e.getMessage()
            );
        }
        catch (Exception e) {

            return Map.of(
                    "success", false,
                    "errorCode", "STATUS_LOOKUP_FAILED"
            );
        }
    }


    private String normalizeCreateError(Exception e) {

        String message = e.getMessage();

        if (message == null) {
            return "MEAL_REQUEST_CREATE_FAILED";
        }

        if (message.contains("회식 예정일")) {
            return "MEAL_DATE_REQUIRED";
        }

        if (message.contains("참석 인원")) {
            return "ATTENDEE_COUNT_INVALID";
        }

        if (message.contains("회식 사유")) {
            return "REASON_REQUIRED";
        }

        if (message.contains("심야") || message.contains("주류")) {
            return "SPECIAL_REASON_REQUIRED";
        }

        if (message.contains("권한")) {
            return "CREATE_ACCESS_DENIED";
        }

        return "MEAL_REQUEST_CREATE_FAILED";
    }


    private String normalizeSubmitError(Exception e) {

        String message = e.getMessage();

        if (message == null) {
            return "MEAL_REQUEST_SUBMIT_FAILED";
        }

        if (message.equals("MEAL_REQUEST_NOT_FOUND")
                || message.equals("REQUEST_OWNER_MISMATCH")
                || message.equals("DETAILS_ALREADY_SUBMITTED")) {
            return message;
        }

        String fieldError = normalizeCreateError(e);

        if (!fieldError.equals("MEAL_REQUEST_CREATE_FAILED")) {
            return fieldError;
        }

        return "MEAL_REQUEST_SUBMIT_FAILED";
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
                request.mealDate() == null ? "" : request.mealDate().toString()
        );

        result.put(
                "attendeeCount",
                request.attendeeCount()
        );

        result.put(
                "mealType",
                request.mealType() == null ? "" : request.mealType().name()
        );

        result.put(
                "mealTypeLabel",
                request.mealType() == null ? "" : request.mealType().label()
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