package com.project.freecruting.repository;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyJoinRequest;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.RequestStatus;
import com.project.freecruting.model.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PartyJoinRequestRepositoryTest {

    @Autowired
    private PartyJoinRequestRepository partyJoinRequestRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Party party;

    @BeforeEach
    void setUp() {
        user1 = entityManager.persistAndFlush(User.builder()
                .name("사용자1")
                .email("user1@example.com")
                .role(Role.USER)
                .build());
        user2 = entityManager.persistAndFlush(User.builder()
                .name("사용자2")
                .email("user2@example.com")
                .role(Role.USER)
                .build());
        party = entityManager.persistAndFlush(Party.builder()
                .name("테스트파티")
                .description("설명")
                .owner_id(user1.getId())
                .max_number(10)
                .build());
    }

    private PartyJoinRequest saveRequest(User user, RequestStatus status) {
        return partyJoinRequestRepository.save(PartyJoinRequest.builder()
                .party(party)
                .user(user)
                .party_role("개발자")
                .status(status)
                .build());
    }

    // ──────────────────────────────────────────
    // findByPartyIdAndUserId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPartyIdAndUserId()")
    class FindByPartyIdAndUserId {

        @Test
        @DisplayName("파티와 사용자가 일치하는 요청을 반환한다")
        void findByPartyIdAndUserId_returnsMatchingRequest() {
            saveRequest(user1, RequestStatus.PENDING);

            Optional<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndUserId(party.getId(), user1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("요청이 없으면 빈 Optional을 반환한다")
        void findByPartyIdAndUserId_noRequest_returnsEmpty() {
            Optional<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndUserId(party.getId(), user1.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 요청과 혼동되지 않는다")
        void findByPartyIdAndUserId_differentUser_returnsEmpty() {
            saveRequest(user2, RequestStatus.PENDING); // user2 만 신청

            Optional<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndUserId(party.getId(), user1.getId());

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findByPartyIdAndUserIdAndStatus()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPartyIdAndUserIdAndStatus()")
    class FindByPartyIdAndUserIdAndStatus {

        @Test
        @DisplayName("파티·사용자·상태가 모두 일치하는 요청을 반환한다")
        void findByPartyIdAndUserIdAndStatus_returnsMatchingRequest() {
            saveRequest(user1, RequestStatus.PENDING);

            Optional<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndUserIdAndStatus(party.getId(), user1.getId(), RequestStatus.PENDING);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("상태가 다르면 빈 Optional을 반환한다")
        void findByPartyIdAndUserIdAndStatus_wrongStatus_returnsEmpty() {
            saveRequest(user1, RequestStatus.PENDING);

            Optional<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndUserIdAndStatus(party.getId(), user1.getId(), RequestStatus.APPROVED);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("REJECTED 상태의 요청을 정확히 조회한다")
        void findByPartyIdAndUserIdAndStatus_rejectedStatus_returnsCorrectly() {
            saveRequest(user1, RequestStatus.REJECTED);

            Optional<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndUserIdAndStatus(party.getId(), user1.getId(), RequestStatus.REJECTED);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RequestStatus.REJECTED);
        }
    }

    // ──────────────────────────────────────────
    // findByPartyIdAndStatus()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPartyIdAndStatus()")
    class FindByPartyIdAndStatus {

        @Test
        @DisplayName("해당 파티의 PENDING 요청만 반환한다")
        void findByPartyIdAndStatus_returnsOnlyMatchingStatus() {
            saveRequest(user1, RequestStatus.PENDING);
            saveRequest(user2, RequestStatus.APPROVED);

            List<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndStatus(party.getId(), RequestStatus.PENDING);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(result.get(0).getUser().getId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("여러 PENDING 요청을 모두 반환한다")
        void findByPartyIdAndStatus_multiplePending_returnsAll() {
            saveRequest(user1, RequestStatus.PENDING);
            saveRequest(user2, RequestStatus.PENDING);

            List<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndStatus(party.getId(), RequestStatus.PENDING);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(r -> r.getStatus() == RequestStatus.PENDING);
        }

        @Test
        @DisplayName("해당 상태의 요청이 없으면 빈 리스트를 반환한다")
        void findByPartyIdAndStatus_noMatch_returnsEmpty() {
            saveRequest(user1, RequestStatus.APPROVED);

            List<PartyJoinRequest> result = partyJoinRequestRepository
                    .findByPartyIdAndStatus(party.getId(), RequestStatus.PENDING);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // setStatus() — JPQL Bulk UPDATE
    // flush + clear 로 1차 캐시 무효화 후 검증
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("setStatus()")
    class SetStatus {

        @Test
        @DisplayName("setStatus() 호출 시 요청의 상태가 변경된다")
        void setStatus_updatesStatusCorrectly() {
            PartyJoinRequest request = saveRequest(user1, RequestStatus.PENDING);
            entityManager.flush();

            partyJoinRequestRepository.setStatus(request.getId(), RequestStatus.APPROVED);
            entityManager.clear();

            PartyJoinRequest updated = partyJoinRequestRepository.findById(request.getId()).get();
            assertThat(updated.getStatus()).isEqualTo(RequestStatus.APPROVED);
        }

        @Test
        @DisplayName("APPROVED → REJECTED 로도 변경된다")
        void setStatus_fromApprovedToRejected() {
            PartyJoinRequest request = saveRequest(user1, RequestStatus.APPROVED);
            entityManager.flush();

            partyJoinRequestRepository.setStatus(request.getId(), RequestStatus.REJECTED);
            entityManager.clear();

            PartyJoinRequest updated = partyJoinRequestRepository.findById(request.getId()).get();
            assertThat(updated.getStatus()).isEqualTo(RequestStatus.REJECTED);
        }

        @Test
        @DisplayName("다른 요청의 상태는 변경되지 않는다")
        void setStatus_doesNotAffectOtherRequests() {
            PartyJoinRequest request1 = saveRequest(user1, RequestStatus.PENDING);
            PartyJoinRequest request2 = saveRequest(user2, RequestStatus.PENDING);
            entityManager.flush();

            partyJoinRequestRepository.setStatus(request1.getId(), RequestStatus.APPROVED);
            entityManager.clear();

            PartyJoinRequest unchanged = partyJoinRequestRepository.findById(request2.getId()).get();
            assertThat(unchanged.getStatus()).isEqualTo(RequestStatus.PENDING);
        }
    }
}
