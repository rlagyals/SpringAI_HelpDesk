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

    @Test
    void 신청_의사만으로_접수번호가_즉시_발급된다() {
        var draft = service.startRequest("member-a1");

        assertThat(draft.requestId()).startsWith("MR-");
        assertThat(draft.status()).isEqualTo(RequestStatus.PENDING_DETAILS);
        assertThat(draft.mealDate()).isNull();
        assertThat(draft.mealType()).isNull();
    }

    @Test
    void 사전_신청서가_없으면_manager가_승인할수없다() {
        var draft = service.startRequest("member-a1");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.approveByManager("manager-a", draft.requestId())
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 사전_신청서_작성후_승인대기_상태로_전환된다() {
        var draft = service.startRequest("member-a1");

        var submitted = service.submitDetails(
                "member-a1",
                draft.requestId(),
                LocalDate.now().plusDays(2),
                4,
                MealType.REGULAR,
                "정기 회식",
                false,
                false,
                ""
        );

        assertThat(submitted.status()).isEqualTo(RequestStatus.PENDING_MANAGER_APPROVAL);
        assertThat(submitted.expectedAmount()).isEqualTo(160_000);
    }

    @Test
    void 본인이_아니면_사전_신청서를_작성할수없다() {
        var draft = service.startRequest("member-a1");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.submitDetails(
                        "member-b1",
                        draft.requestId(),
                        LocalDate.now().plusDays(2),
                        4,
                        MealType.REGULAR,
                        "정기 회식",
                        false,
                        false,
                        ""
                )
        ).isInstanceOf(IllegalStateException.class);
    }
}
