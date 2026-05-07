package com.project.freecruting.service;

import com.project.freecruting.dto.party.PartyJoinRequestSaveRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyJoinRequest;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.RequestStatus;
import com.project.freecruting.model.type.Role;
import com.project.freecruting.repository.PartyJoinRequestRepository;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.repository.PartyRepository;
import com.project.freecruting.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartyJoinRequestServiceTest {

    @InjectMocks
    private PartyJoinRequestService partyJoinRequestService;

    @Mock
    private PartyJoinRequestRepository partyJoinRequestRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PartyRepository partyRepository;

    private User createUser(Long id, String name) {
        User user = User.builder()
                .email("user@example.com").name(name).password("encoded")
                .role(Role.USER).provider("local").picture("favicon.ico").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Party createParty(Long id, Long ownerId, int maxNumber) {
        Party party = Party.builder()
                .name("테스트 파티").description("설명").owner_id(ownerId).max_number(maxNumber).build();
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }

    private PartyJoinRequest createJoinRequest(Long id, Party party, User user, RequestStatus status) {
        PartyJoinRequest request = PartyJoinRequest.builder()
                .party_role("developer").party(party).user(user).status(status).build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    // ──────────────────────────────────────────
    // save()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("가입 신청 성공 시 PartyJoinRequest ID를 반환한다")
        void save_success_returnsId() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "신청자");
            Party party = createParty(partyId, 99L, 10);
            PartyJoinRequestSaveRequestDto dto = PartyJoinRequestSaveRequestDto.builder()
                    .party_id(partyId).party_role("developer").build();
            PartyJoinRequest saved = createJoinRequest(5L, party, user, RequestStatus.PENDING);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findByPartyIdAndUserIdAndStatus(partyId, userId, RequestStatus.PENDING))
                    .willReturn(Optional.empty());
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId)).willReturn(Optional.empty());
            given(partyJoinRequestRepository.save(any(PartyJoinRequest.class))).willReturn(saved);

            Long result = partyJoinRequestService.save(dto, userId);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("존재하지 않는 USER이면 NotFoundException 발생")
        void save_userNotFound_throwsNotFoundException() {
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());
            PartyJoinRequestSaveRequestDto dto = PartyJoinRequestSaveRequestDto.builder()
                    .party_id(10L).party_role("developer").build();

            assertThatThrownBy(() -> partyJoinRequestService.save(dto, 999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void save_partyNotFound_throwsNotFoundException() {
            User user = createUser(1L, "신청자");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());

            PartyJoinRequestSaveRequestDto dto = PartyJoinRequestSaveRequestDto.builder()
                    .party_id(999L).party_role("developer").build();

            assertThatThrownBy(() -> partyJoinRequestService.save(dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("이미 PENDING 상태의 신청이 있으면 InvalidStateException 발생")
        void save_alreadyPending_throwsInvalidStateException() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "신청자");
            Party party = createParty(partyId, 99L, 10);
            PartyJoinRequest existing = createJoinRequest(1L, party, user, RequestStatus.PENDING);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findByPartyIdAndUserIdAndStatus(partyId, userId, RequestStatus.PENDING))
                    .willReturn(Optional.of(existing));

            PartyJoinRequestSaveRequestDto dto = PartyJoinRequestSaveRequestDto.builder()
                    .party_id(partyId).party_role("developer").build();

            assertThatThrownBy(() -> partyJoinRequestService.save(dto, userId))
                    .isInstanceOf(InvalidStateException.class);
        }

        @Test
        @DisplayName("이미 파티 멤버이면 InvalidStateException 발생")
        void save_alreadyMember_throwsInvalidStateException() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "이미 멤버");
            Party party = createParty(partyId, 99L, 10);
            PartyMember existingMember = mock(PartyMember.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findByPartyIdAndUserIdAndStatus(partyId, userId, RequestStatus.PENDING))
                    .willReturn(Optional.empty());
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId))
                    .willReturn(Optional.of(existingMember));

            PartyJoinRequestSaveRequestDto dto = PartyJoinRequestSaveRequestDto.builder()
                    .party_id(partyId).party_role("developer").build();

            assertThatThrownBy(() -> partyJoinRequestService.save(dto, userId))
                    .isInstanceOf(InvalidStateException.class);
        }

        @Test
        @DisplayName("파티 인원이 가득 찼으면 InvalidStateException 발생")
        void save_partyFull_throwsInvalidStateException() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "신청자");
            Party party = mock(Party.class);
            given(party.getMax_number()).willReturn(5);
            given(party.getPartyMembers()).willReturn(List.of(
                    mock(PartyMember.class), mock(PartyMember.class), mock(PartyMember.class),
                    mock(PartyMember.class), mock(PartyMember.class)));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findByPartyIdAndUserIdAndStatus(partyId, userId, RequestStatus.PENDING))
                    .willReturn(Optional.empty());
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId)).willReturn(Optional.empty());

            PartyJoinRequestSaveRequestDto dto = PartyJoinRequestSaveRequestDto.builder()
                    .party_id(partyId).party_role("developer").build();

            assertThatThrownBy(() -> partyJoinRequestService.save(dto, userId))
                    .isInstanceOf(InvalidStateException.class);
        }
    }

    // ──────────────────────────────────────────
    // approve()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("owner가 PENDING 요청을 승인하면 APPROVED 처리하고 PartyMember를 생성한다")
        void approve_success() {
            Long ownerId = 10L, requestId = 1L, partyId = 100L;
            Party party = createParty(partyId, ownerId, 5);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.PENDING);

            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(partyMemberRepository.findByPartyId(partyId)).willReturn(List.of()); // 현재 0명

            partyJoinRequestService.approve(requestId, ownerId);

            verify(partyJoinRequestRepository).setStatus(requestId, RequestStatus.APPROVED);
            verify(partyMemberRepository).save(any(PartyMember.class));
        }

        @Test
        @DisplayName("존재하지 않는 요청이면 NotFoundException 발생")
        void approve_notFound_throwsNotFoundException() {
            given(partyJoinRequestRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyJoinRequestService.approve(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("owner가 아닌 사용자가 승인하면 ForbiddenException 발생")
        void approve_notOwner_throwsForbiddenException() {
            Long requestId = 1L, ownerId = 10L;
            Party party = createParty(100L, ownerId, 5);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.PENDING);

            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> partyJoinRequestService.approve(requestId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("이미 처리된 요청(APPROVED)이면 InvalidStateException 발생")
        void approve_alreadyProcessed_throwsInvalidStateException() {
            Long ownerId = 10L, requestId = 1L;
            Party party = createParty(100L, ownerId, 5);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.APPROVED);

            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> partyJoinRequestService.approve(requestId, ownerId))
                    .isInstanceOf(InvalidStateException.class);
        }

        @Test
        @DisplayName("파티 인원이 가득 찼으면 승인 시 InvalidStateException 발생")
        void approve_partyFull_throwsInvalidStateException() {
            Long ownerId = 10L, requestId = 1L, partyId = 100L;
            Party party = createParty(partyId, ownerId, 2);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.PENDING);
            // 현재 멤버 2명 (max_number=2)
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(partyMemberRepository.findByPartyId(partyId)).willReturn(
                    List.of(mock(PartyMember.class), mock(PartyMember.class)));

            assertThatThrownBy(() -> partyJoinRequestService.approve(requestId, ownerId))
                    .isInstanceOf(InvalidStateException.class);
        }
    }

    // ──────────────────────────────────────────
    // reject()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("owner가 PENDING 요청을 거절하면 REJECTED 처리한다")
        void reject_success() {
            Long ownerId = 10L, requestId = 1L;
            Party party = createParty(100L, ownerId, 5);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.PENDING);

            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            partyJoinRequestService.reject(requestId, ownerId);

            verify(partyJoinRequestRepository).setStatus(requestId, RequestStatus.REJECTED);
        }

        @Test
        @DisplayName("존재하지 않는 요청이면 NotFoundException 발생")
        void reject_notFound_throwsNotFoundException() {
            given(partyJoinRequestRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyJoinRequestService.reject(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("owner가 아닌 사용자가 거절하면 ForbiddenException 발생")
        void reject_notOwner_throwsForbiddenException() {
            Long requestId = 1L, ownerId = 10L;
            Party party = createParty(100L, ownerId, 5);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.PENDING);

            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> partyJoinRequestService.reject(requestId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("이미 처리된 요청(REJECTED)이면 InvalidStateException 발생")
        void reject_alreadyProcessed_throwsInvalidStateException() {
            Long ownerId = 10L, requestId = 1L;
            Party party = createParty(100L, ownerId, 5);
            User applicant = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(requestId, party, applicant, RequestStatus.REJECTED);

            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            assertThatThrownBy(() -> partyJoinRequestService.reject(requestId, ownerId))
                    .isInstanceOf(InvalidStateException.class);
        }
    }

    // ──────────────────────────────────────────
    // findPendingByPartyId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findPendingByPartyId()")
    class FindPendingByPartyId {

        @Test
        @DisplayName("파티의 PENDING 상태 요청 목록을 반환한다")
        void findPendingByPartyId_returnsList() {
            Party party = createParty(1L, 10L, 5);
            User user = createUser(20L, "신청자");
            PartyJoinRequest request = createJoinRequest(1L, party, user, RequestStatus.PENDING);
            given(partyJoinRequestRepository.findByPartyIdAndStatus(1L, RequestStatus.PENDING))
                    .willReturn(List.of(request));

            var result = partyJoinRequestService.findPendingByPartyId(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("PENDING 신청이 없으면 빈 리스트를 반환한다")
        void findPendingByPartyId_noPending_returnsEmptyList() {
            given(partyJoinRequestRepository.findByPartyIdAndStatus(anyLong(), eq(RequestStatus.PENDING)))
                    .willReturn(Collections.emptyList());

            var result = partyJoinRequestService.findPendingByPartyId(1L);

            assertThat(result).isEmpty();
        }
    }
}
