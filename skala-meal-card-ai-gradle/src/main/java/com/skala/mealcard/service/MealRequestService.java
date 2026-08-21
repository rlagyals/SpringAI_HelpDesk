package com.skala.mealcard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.skala.mealcard.domain.MealRequest;
import com.skala.mealcard.domain.MealType;
import com.skala.mealcard.domain.RequestStatus;
import com.skala.mealcard.domain.Team;
import com.skala.mealcard.domain.UserAccount;
import com.skala.mealcard.domain.UserRole;
import com.skala.mealcard.repository.MealRequestRepository;
import com.skala.mealcard.repository.TeamRepository;
import com.skala.mealcard.repository.UserRepository;

@Service
public class MealRequestService {

        private final UserRepository users;
        private final TeamRepository teams;
        private final MealRequestRepository requests;

        public MealRequestService(
                UserRepository users,
                TeamRepository teams,
                MealRequestRepository requests) {
                this.users = users;
                this.teams = teams;
                this.requests = requests;
        }

        public Team requireUserTeam(String userId) {
                UserAccount user = requireUser(userId);

                return teams.findById(user.teamId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("팀 정보를 찾을 수 없습니다."));
        }

        public UserAccount requireUser(String userId) {
                return users.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        }

        public MealRequest createRequest(
                String userId,
                LocalDate mealDate,
                int attendeeCount,
                MealType mealType,
                String reason,
                boolean lateNightPlanned,
                boolean alcoholPlanned,
                String specialReason) {

                MealRequest draft = startRequest(userId);

                return submitDetails(
                        userId,
                        draft.requestId(),
                        mealDate,
                        attendeeCount,
                        mealType,
                        reason,
                        lateNightPlanned,
                        alcoholPlanned,
                        specialReason
                );
        }

        public MealRequest startRequest(String userId) {

                UserAccount user = requireUser(userId);
                Team team = requireUserTeam(userId);

                if (user.role() != UserRole.MEMBER
                        && user.role() != UserRole.MANAGER) {
                throw new IllegalStateException("회식 신청 권한이 없습니다.");
                }

                String requestId =
                        "MR-" + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();

                MealRequest request = new MealRequest(
                        requestId,
                        team.teamId(),
                        user.userId(),
                        null,
                        0,
                        null,
                        null,
                        0,
                        false,
                        false,
                        "",
                        RequestStatus.PENDING_DETAILS,
                        null,
                        LocalDateTime.now(),
                        null
                );

                return requests.save(request);
        }

        public MealRequest submitDetails(
                String userId,
                String requestId,
                LocalDate mealDate,
                int attendeeCount,
                MealType mealType,
                String reason,
                boolean lateNightPlanned,
                boolean alcoholPlanned,
                String specialReason) {

                MealRequest request = requests.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("MEAL_REQUEST_NOT_FOUND"));

                if (!request.applicantUserId().equals(userId)) {
                throw new IllegalStateException("REQUEST_OWNER_MISMATCH");
                }

                if (request.status() != RequestStatus.PENDING_DETAILS) {
                throw new IllegalStateException("DETAILS_ALREADY_SUBMITTED");
                }

                validateDetails(
                        mealDate, attendeeCount, mealType, reason,
                        lateNightPlanned, alcoholPlanned, specialReason
                );

                int expectedAmount =
                        attendeeCount * mealType.perPersonLimit();

                MealRequest updated = request.withDetails(
                        mealDate,
                        attendeeCount,
                        mealType,
                        reason,
                        expectedAmount,
                        lateNightPlanned,
                        alcoholPlanned,
                        specialReason == null ? "" : specialReason,
                        RequestStatus.PENDING_MANAGER_APPROVAL
                );

                return requests.save(updated);
        }

        private void validateDetails(
                LocalDate mealDate,
                int attendeeCount,
                MealType mealType,
                String reason,
                boolean lateNightPlanned,
                boolean alcoholPlanned,
                String specialReason) {

                if (mealDate == null) {
                throw new IllegalArgumentException("회식 예정일은 필수입니다.");
                }

                if (attendeeCount <= 0) {
                throw new IllegalArgumentException(
                        "참석 인원 수는 1명 이상이어야 합니다.");
                }

                if (mealType == null) {
                throw new IllegalArgumentException("회식 유형은 필수입니다.");
                }

                if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("회식 사유는 필수입니다.");
                }

                if ((lateNightPlanned || alcoholPlanned)
                        && (specialReason == null || specialReason.isBlank())) {
                throw new IllegalArgumentException(
                        "심야 또는 주류 예정 시 사유가 필요합니다.");
                }
        }

        public MealRequest getRequestStatus(
        String requestId,
        String userId) {

        if (requestId == null || requestId.isBlank()) {
                throw new IllegalArgumentException("REQUEST_ID_REQUIRED");
        }

        MealRequest request = requests.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException("MEAL_REQUEST_NOT_FOUND")
                );

        UserAccount user = requireUser(userId);

        if (!request.teamId().equals(user.teamId())) {
                throw new IllegalStateException("TEAM_ACCESS_DENIED");
        }

        return request;
}

        public MealRequest approveByManager(
                String managerUserId,
                String requestId) {

                UserAccount manager = requireUser(managerUserId);

                if (manager.role() != UserRole.MANAGER) {
                throw new IllegalStateException(
                        "MANAGER만 승인할 수 있습니다.");
                }

                MealRequest request = requests.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "신청을 찾을 수 없습니다."));
                
                if (!request.teamId().equals(manager.teamId())) {
                throw new IllegalStateException(
                        "다른 팀의 신청은 승인할 수 없습니다.");
                }

                if (request.status() == RequestStatus.PENDING_DETAILS) {
                throw new IllegalStateException(
                        "사전 신청서가 아직 작성되지 않아 승인할 수 없습니다.");
                }

                if (request.status()
                        != RequestStatus.PENDING_MANAGER_APPROVAL) {
                throw new IllegalStateException(
                        "현재 상태에서는 승인할 수 없습니다.");
                }

                Team team = teams.findById(manager.teamId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "팀 정보를 찾을 수 없습니다."));

                // 원 규정에는 잔여 예산 초과 시 부서장 추가 승인이 필요하다.
                // 이번 과제 범위는 MEMBER/MANAGER만 구현하므로,
                // 규정을 임의 변경하지 않고 외부 상위 승인 필요 상태로 남긴다.
                if (request.expectedAmount() > team.remainingBudget()) {

                MealRequest external = request.withStatus(
                        RequestStatus.EXTERNAL_APPROVAL_REQUIRED,
                        null,
                        null
                );

                return requests.save(external);
                }

                String approvalNumber =
                        "AP-" + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();

                MealRequest approved = request.withStatus(
                        RequestStatus.APPROVED,
                        approvalNumber,
                        LocalDateTime.now()
                );

                return requests.save(approved);
        }

        public MealRequest rejectByManager(
                String managerUserId,
                String requestId) {

                UserAccount manager = requireUser(managerUserId);

                if (manager.role() != UserRole.MANAGER) {
                throw new IllegalStateException(
                        "MANAGER만 반려할 수 있습니다.");
                }

                MealRequest request = requests.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "신청을 찾을 수 없습니다."));

                if (!request.teamId().equals(manager.teamId())) {
                throw new IllegalStateException(
                        "다른 팀의 신청은 반려할 수 없습니다.");
                }

                MealRequest rejected = request.withStatus(
                        RequestStatus.REJECTED,
                        null,
                        null
                );

                return requests.save(rejected);
        }
        }