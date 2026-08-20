package com.skala.mealcard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skala.mealcard.domain.MealType;
import com.skala.mealcard.domain.RequestStatus;
import com.skala.mealcard.repository.MealRequestRepository;
import com.skala.mealcard.repository.TeamRepository;
import com.skala.mealcard.repository.UserRepository;

class MealRequestServiceTest {

    private MealRequestService service;

    @BeforeEach
    void setUp() {
        service = new MealRequestService(
                new UserRepository(),
                new TeamRepository(),
                new MealRequestRepository()
        );
    }

    @Test
    void member가_회식_사전신청을_생성한다() {
        var request = service.createRequest(
                "member-a1",
                LocalDate.now().plusDays(2),
                4,
                MealType.REGULAR,
                "정기 회식",
                false,
                false,
                ""
        );

        assertThat(request.expectedAmount()).isEqualTo(160_000);
        assertThat(request.status()).isEqualTo(RequestStatus.PENDING_MANAGER_APPROVAL);
    }

    @Test
    void 같은팀_manager가_예산이내_신청을_승인한다() {
        var request = service.createRequest(
                "member-a1",
                LocalDate.now().plusDays(2),
                4,
                MealType.REGULAR,
                "정기 회식",
                false,
                false,
                ""
        );

        var approved = service.approveByManager("manager-a", request.requestId());

        assertThat(approved.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(approved.approvalNumber()).startsWith("AP-");
    }

    @Test
    void 다른팀_manager는_승인할수없다() {
        var request = service.createRequest(
                "member-a1",
                LocalDate.now().plusDays(2),
                4,
                MealType.REGULAR,
                "정기 회식",
                false,
                false,
                ""
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.approveByManager("manager-b", request.requestId())
        ).isInstanceOf(IllegalStateException.class);
    }
}
