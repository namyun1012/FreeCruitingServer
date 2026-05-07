package com.project.freecruting.service;

import com.project.freecruting.dto.party.PartySaveRequestDto;
import com.project.freecruting.dto.party.PartyResponseDto;
import com.project.freecruting.dto.party.PartyUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
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
class PartyServiceTest {

    @InjectMocks
    private PartyService partyService;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private UserRepository userRepository;

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
        // PartyListResponseDto 생성 시 getPartyMembers().size() 호출 → NPE 방지
        ReflectionTestUtils.setField(party, "partyMembers", Collections.emptyList());
        return party;
    }

    private PartyMember createPartyMember(Long id, Party party, User user) {
        PartyMember member = PartyMember.builder()
                .party_role("owner").party(party).user(user).build();
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
        @DisplayName("저장 성공 시 Party ID를 반환하고 본인을 owner로 PartyMember에 추가한다")
        void save_success_returnsIdAndAddsMemberAsOwner() {
            Long userId = 1L;
            User user = createUser(userId, "팀장");
            Party savedParty = createParty(10L, userId, 5);
            PartyMember savedMember = createPartyMember(1L, savedParty, user);

            PartySaveRequestDto dto = PartySaveRequestDto.builder()
                    .name("테스트 파티").description("설명").max_number(5).build();

            given(partyRepository.save(any(Party.class))).willReturn(savedParty);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(partyMemberRepository.save(any(PartyMember.class))).willReturn(savedMember);

            Long result = partyService.save(dto, userId);

            assertThat(result).isEqualTo(10L);
            verify(partyMemberRepository).save(any(PartyMember.class));
        }

        @Test
        @DisplayName("user가 존재하지 않으면 NotFoundException 발생")
        void save_userNotFound_throwsNotFoundException() {
            PartySaveRequestDto dto = PartySaveRequestDto.builder()
                    .name("파티").description("설명").max_number(5).build();
            Party savedParty = createParty(10L, 999L, 5);

            given(partyRepository.save(any(Party.class))).willReturn(savedParty);
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.save(dto, 999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("owner가 수정하면 정보가 변경되고 ID를 반환한다")
        void update_byOwner_success() {
            Long partyId = 1L, ownerId = 10L;
            Party party = createParty(partyId, ownerId, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            PartyUpdateRequestDto dto = PartyUpdateRequestDto.builder()
                    .name("수정된 파티명").description("수정된 설명").max_number(8).build();

            Long result = partyService.update(partyId, dto, ownerId);

            assertThat(result).isEqualTo(partyId);
            assertThat(party.getName()).isEqualTo("수정된 파티명");
            assertThat(party.getMax_number()).isEqualTo(8);
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void update_notFound_throwsNotFoundException() {
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());
            PartyUpdateRequestDto dto = PartyUpdateRequestDto.builder()
                    .name("이름").description("설명").max_number(5).build();

            assertThatThrownBy(() -> partyService.update(999L, dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("owner가 아닌 사용자가 수정하면 ForbiddenException 발생")
        void update_notOwner_throwsForbiddenException() {
            Long partyId = 1L;
            Party party = createParty(partyId, 10L, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            PartyUpdateRequestDto dto = PartyUpdateRequestDto.builder()
                    .name("이름").description("설명").max_number(5).build();

            assertThatThrownBy(() -> partyService.update(partyId, dto, 99L))
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
        @DisplayName("owner가 삭제하면 delete()가 호출되고 ID를 반환한다")
        void delete_byOwner_success() {
            Long partyId = 1L, ownerId = 10L;
            Party party = createParty(partyId, ownerId, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            Long result = partyService.delete(partyId, ownerId);

            assertThat(result).isEqualTo(partyId);
            verify(partyRepository).delete(party);
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void delete_notFound_throwsNotFoundException() {
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.delete(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("owner가 아닌 사용자가 삭제하면 ForbiddenException 발생")
        void delete_notOwner_throwsForbiddenException() {
            Long partyId = 1L;
            Party party = createParty(partyId, 10L, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            assertThatThrownBy(() -> partyService.delete(partyId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ──────────────────────────────────────────
    // findAllPage()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAllPage()")
    class FindAllPage {

        @Test
        @DisplayName("전체 파티 목록을 페이징하여 반환한다")
        void findAllPage_returnsPage() {
            Party party = createParty(1L, 10L, 5);
            Page<Party> page = new PageImpl<>(List.of(party));
            given(partyRepository.findAll(any(Pageable.class))).willReturn(page);

            var result = partyService.findAllPage(0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("파티가 없으면 빈 페이지를 반환한다")
        void findAllPage_empty_returnsEmptyPage() {
            given(partyRepository.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(Collections.emptyList()));

            var result = partyService.findAllPage(0, 10);

            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────
    // findById()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("존재하는 Party ID로 조회하면 PartyResponseDto를 반환한다")
        void findById_success() {
            Party party = createParty(1L, 10L, 5);
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));

            PartyResponseDto result = partyService.findById(1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void findById_notFound_throwsNotFoundException() {
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // findByIdForOwner()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByIdForOwner()")
    class FindByIdForOwner {

        @Test
        @DisplayName("owner가 조회하면 PartyResponseDto를 반환한다")
        void findByIdForOwner_byOwner_success() {
            Long partyId = 1L, ownerId = 10L;
            Party party = createParty(partyId, ownerId, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            PartyResponseDto result = partyService.findByIdForOwner(partyId, ownerId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void findByIdForOwner_notFound_throwsNotFoundException() {
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.findByIdForOwner(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("owner가 아니면 ForbiddenException 발생")
        void findByIdForOwner_notOwner_throwsForbiddenException() {
            Long partyId = 1L;
            Party party = createParty(partyId, 10L, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            assertThatThrownBy(() -> partyService.findByIdForOwner(partyId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ──────────────────────────────────────────
    // findByIdForMember()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByIdForMember()")
    class FindByIdForMember {

        @Test
        @DisplayName("파티 구성원이 조회하면 PartyResponseDto를 반환한다")
        void findByIdForMember_byMember_success() {
            Long partyId = 1L, userId = 5L;
            Party party = createParty(partyId, 10L, 5);
            User user = createUser(userId, "멤버");
            PartyMember member = createPartyMember(1L, party, user);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, userId))
                    .willReturn(Optional.of(member));

            PartyResponseDto result = partyService.findByIdForMember(partyId, userId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 Party이면 NotFoundException 발생")
        void findByIdForMember_partyNotFound_throwsNotFoundException() {
            given(partyRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.findByIdForMember(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("파티 구성원이 아니면 ForbiddenException 발생")
        void findByIdForMember_notMember_throwsForbiddenException() {
            Long partyId = 1L;
            Party party = createParty(partyId, 10L, 5);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyIdAndUserId(partyId, 99L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.findByIdForMember(partyId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
