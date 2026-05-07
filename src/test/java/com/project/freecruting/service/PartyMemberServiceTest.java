package com.project.freecruting.service;

import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.Role;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
class PartyMemberServiceTest {

    @InjectMocks
    private PartyMemberService partyMemberService;

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

    private PartyMember createPartyMember(Long id, Party party, User user, String role) {
        PartyMember member = PartyMember.builder()
                .party_role(role).party(party).user(user).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    // ──────────────────────────────────────────
    // save()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("저장 성공 시 PartyMember ID를 반환한다")
        void save_success_returnsId() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "멤버");
            Party party = createParty(partyId, 99L, 10);
            PartyMemberSaveRequestDto dto = new PartyMemberSaveRequestDto("member", partyId);
            PartyMember saved = createPartyMember(5L, party, user, "member");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId)).willReturn(Optional.empty());
            given(partyMemberRepository.save(any(PartyMember.class))).willReturn(saved);

            Long result = partyMemberService.save(dto, userId);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("존재하지 않는 USER이면 NotFoundException 발생")
        void save_userNotFound_throwsNotFoundException() {
            PartyMemberSaveRequestDto dto = new PartyMemberSaveRequestDto("member", 10L);
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyMemberService.save(dto, 999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void save_partyNotFound_throwsNotFoundException() {
            User user = createUser(1L, "멤버");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());

            PartyMemberSaveRequestDto dto = new PartyMemberSaveRequestDto("member", 999L);

            assertThatThrownBy(() -> partyMemberService.save(dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("이미 참가한 유저이면 InvalidStateException 발생")
        void save_alreadyMember_throwsInvalidStateException() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "멤버");
            Party party = createParty(partyId, 99L, 10);
            PartyMember existing = createPartyMember(1L, party, user, "member");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId))
                    .willReturn(Optional.of(existing));

            PartyMemberSaveRequestDto dto = new PartyMemberSaveRequestDto("member", partyId);

            assertThatThrownBy(() -> partyMemberService.save(dto, userId))
                    .isInstanceOf(InvalidStateException.class);
        }

        @Test
        @DisplayName("파티 인원이 가득 찼으면 InvalidStateException 발생")
        void save_partyFull_throwsInvalidStateException() {
            Long userId = 1L, partyId = 10L;
            User user = createUser(userId, "신규");
            // 현재 멤버 5명인 파티를 max_number=5로 설정 (List mock 필요)
            Party party = mock(Party.class);
            given(party.getMax_number()).willReturn(5);
            // getPartyMembers()가 5명짜리 리스트 반환
            given(party.getPartyMembers()).willReturn(List.of(
                    mock(PartyMember.class), mock(PartyMember.class), mock(PartyMember.class),
                    mock(PartyMember.class), mock(PartyMember.class)));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId)).willReturn(Optional.empty());

            PartyMemberSaveRequestDto dto = new PartyMemberSaveRequestDto("member", partyId);

            assertThatThrownBy(() -> partyMemberService.save(dto, userId))
                    .isInstanceOf(InvalidStateException.class);
        }
    }

    // ──────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("본인이 역할을 수정하면 party_role이 변경되고 ID를 반환한다")
        void update_bySelf_success() {
            Long memberId = 1L, userId = 5L, ownerId = 10L;
            Party party = createParty(100L, ownerId, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(memberId, party, user, "developer");
            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(member));

            PartyMemberUpdateRequestDto dto = PartyMemberUpdateRequestDto.builder()
                    .party_role("designer").build();

            Long result = partyMemberService.update(memberId, dto, userId);

            assertThat(result).isEqualTo(memberId);
            assertThat(member.getParty_role()).isEqualTo("designer");
        }

        @Test
        @DisplayName("파티 owner가 역할을 수정하면 변경되고 ID를 반환한다")
        void update_byOwner_success() {
            Long memberId = 1L, userId = 5L, ownerId = 10L;
            Party party = createParty(100L, ownerId, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(memberId, party, user, "developer");
            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(member));

            PartyMemberUpdateRequestDto dto = PartyMemberUpdateRequestDto.builder()
                    .party_role("designer").build();

            Long result = partyMemberService.update(memberId, dto, ownerId);

            assertThat(result).isEqualTo(memberId);
        }

        @Test
        @DisplayName("존재하지 않는 PartyMember이면 NotFoundException 발생")
        void update_notFound_throwsNotFoundException() {
            given(partyMemberRepository.findById(anyLong())).willReturn(Optional.empty());
            PartyMemberUpdateRequestDto dto = PartyMemberUpdateRequestDto.builder()
                    .party_role("developer").build();

            assertThatThrownBy(() -> partyMemberService.update(999L, dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("본인도 owner도 아닌 사용자가 수정하면 ForbiddenException 발생")
        void update_notSelfOrOwner_throwsForbiddenException() {
            Long memberId = 1L, userId = 5L, ownerId = 10L;
            Party party = createParty(100L, ownerId, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(memberId, party, user, "developer");
            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(member));

            PartyMemberUpdateRequestDto dto = PartyMemberUpdateRequestDto.builder()
                    .party_role("designer").build();

            assertThatThrownBy(() -> partyMemberService.update(memberId, dto, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ──────────────────────────────────────────
    // delete()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("일반 멤버가 본인을 삭제하면 파티 멤버가 제거되고 ID를 반환한다")
        void delete_bySelf_memberRemoved() {
            Long memberId = 1L, userId = 5L, ownerId = 10L, partyId = 100L;
            Party party = createParty(partyId, ownerId, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(memberId, party, user, "developer");
            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(partyMemberRepository.existsByPartyId(partyId)).willReturn(true); // 아직 파티에 사람 있음

            Long result = partyMemberService.delete(memberId, userId);

            assertThat(result).isEqualTo(memberId);
            verify(partyMemberRepository).delete(member);
            verify(partyRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("owner가 탈퇴하고 파티에 남은 멤버가 있으면 새 owner를 지정한다")
        void delete_ownerLeaves_newOwnerAssigned() {
            Long memberId = 1L, ownerId = 10L, partyId = 100L;
            Party party = createParty(partyId, ownerId, 5);
            User ownerUser = createUser(ownerId, "팀장");
            PartyMember ownerMember = createPartyMember(memberId, party, ownerUser, "owner");

            User newOwnerUser = createUser(20L, "신임팀장");
            PartyMember newOwnerMember = createPartyMember(2L, party, newOwnerUser, "developer");

            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(ownerMember));
            given(partyMemberRepository.existsByPartyId(partyId))
                    .willReturn(true)   // delete 후 체크
                    .willReturn(true);  // 파티 삭제 여부 체크
            given(partyMemberRepository.findRandomPartyMemberByPartyId(partyId))
                    .willReturn(Optional.of(newOwnerMember));

            partyMemberService.delete(memberId, ownerId);

            verify(partyMemberRepository).save(newOwnerMember);
            verify(partyRepository).updateOwnerIdById(newOwnerUser.getId(), partyId);
            verify(partyRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("마지막 멤버가 떠나면 파티가 삭제된다")
        void delete_lastMemberLeaves_partyDeleted() {
            Long memberId = 1L, userId = 5L, ownerId = 10L, partyId = 100L;
            Party party = createParty(partyId, ownerId, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(memberId, party, user, "developer");

            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(member));
            // delete 후 파티에 아무도 없음
            given(partyMemberRepository.existsByPartyId(partyId)).willReturn(false);

            partyMemberService.delete(memberId, userId);

            verify(partyRepository).deleteById(partyId);
        }

        @Test
        @DisplayName("존재하지 않는 PartyMember이면 NotFoundException 발생")
        void delete_notFound_throwsNotFoundException() {
            given(partyMemberRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyMemberService.delete(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("본인도 owner도 아닌 사용자가 삭제하면 ForbiddenException 발생")
        void delete_notSelfOrOwner_throwsForbiddenException() {
            Long memberId = 1L, userId = 5L, ownerId = 10L;
            Party party = createParty(100L, ownerId, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(memberId, party, user, "developer");
            given(partyMemberRepository.findById(memberId)).willReturn(Optional.of(member));

            assertThatThrownBy(() -> partyMemberService.delete(memberId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ──────────────────────────────────────────
    // findByPartyId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPartyId()")
    class FindByPartyId {

        @Test
        @DisplayName("파티 ID로 멤버 목록을 반환한다")
        void findByPartyId_returnsList() {
            Party party = createParty(1L, 10L, 5);
            User user = createUser(5L, "멤버");
            PartyMember member = createPartyMember(1L, party, user, "developer");
            given(partyMemberRepository.findByPartyId(1L)).willReturn(List.of(member));

            var result = partyMemberService.findByPartyId(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("멤버가 없으면 빈 리스트를 반환한다")
        void findByPartyId_noMembers_returnsEmptyList() {
            given(partyMemberRepository.findByPartyId(anyLong())).willReturn(Collections.emptyList());

            var result = partyMemberService.findByPartyId(1L);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findByUserId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("유저 ID로 소속 파티 멤버십 목록을 페이징하여 반환한다")
        void findByUserId_returnsPage() {
            Party party = createParty(1L, 5L, 5);
            User user = createUser(5L, "멤버");
            PartyMember member = createPartyMember(1L, party, user, "developer");
            Page<PartyMember> page = new PageImpl<>(List.of(member));
            given(partyMemberRepository.findByUserId(eq(5L), any(Pageable.class))).willReturn(page);

            var result = partyMemberService.findByUserId(5L, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
