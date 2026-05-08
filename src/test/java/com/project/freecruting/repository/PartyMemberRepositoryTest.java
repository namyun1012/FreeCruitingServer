package com.project.freecruting.repository;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PartyMemberRepositoryTest {

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Party party1;
    private Party party2;

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
        party1 = entityManager.persistAndFlush(Party.builder()
                .name("파티A")
                .description("설명")
                .owner_id(user1.getId())
                .max_number(10)
                .build());
        party2 = entityManager.persistAndFlush(Party.builder()
                .name("파티B")
                .description("설명")
                .owner_id(user2.getId())
                .max_number(10)
                .build());
    }

    private PartyMember saveMember(Party party, User user, String role) {
        return partyMemberRepository.save(PartyMember.builder()
                .party(party)
                .user(user)
                .party_role(role)
                .build());
    }

    // ──────────────────────────────────────────
    // findByPartyIdAndUserId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPartyIdAndUserId()")
    class FindByPartyIdAndUserId {

        @Test
        @DisplayName("파티와 사용자가 일치하는 멤버를 반환한다")
        void findByPartyIdAndUserId_returnsMatchingMember() {
            saveMember(party1, user1, "개발자");

            Optional<PartyMember> result = partyMemberRepository
                    .findByPartyIdAndUserId(party1.getId(), user1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getUser().getId()).isEqualTo(user1.getId());
            assertThat(result.get().getParty().getId()).isEqualTo(party1.getId());
        }

        @Test
        @DisplayName("해당 파티에 없는 사용자는 빈 Optional을 반환한다")
        void findByPartyIdAndUserId_notMember_returnsEmpty() {
            Optional<PartyMember> result = partyMemberRepository
                    .findByPartyIdAndUserId(party1.getId(), user1.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 파티의 멤버와 혼동되지 않는다")
        void findByPartyIdAndUserId_differentParty_returnsEmpty() {
            saveMember(party2, user1, "개발자"); // party2 에만 가입

            Optional<PartyMember> result = partyMemberRepository
                    .findByPartyIdAndUserId(party1.getId(), user1.getId());

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findByPartyId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPartyId()")
    class FindByPartyId {

        @Test
        @DisplayName("해당 파티의 모든 멤버를 반환한다")
        void findByPartyId_returnsAllMembersOfParty() {
            saveMember(party1, user1, "개발자");
            saveMember(party1, user2, "디자이너");
            saveMember(party2, user1, "기획자");

            List<PartyMember> result = partyMemberRepository.findByPartyId(party1.getId());

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(m -> m.getParty().getId() == party1.getId());
        }

        @Test
        @DisplayName("멤버가 없는 파티는 빈 리스트를 반환한다")
        void findByPartyId_empty_returnsEmptyList() {
            List<PartyMember> result = partyMemberRepository.findByPartyId(party1.getId());

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
        @DisplayName("해당 사용자가 속한 파티 목록을 페이징하여 반환한다")
        void findByUserId_returnsPartiesForUser() {
            saveMember(party1, user1, "개발자");
            saveMember(party2, user1, "기획자");
            saveMember(party1, user2, "디자이너"); // user2 는 제외

            Page<PartyMember> page = partyMemberRepository
                    .findByUserId(user1.getId(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allMatch(m -> m.getUser().getId().equals(user1.getId()));
        }

        @Test
        @DisplayName("size만큼만 반환하고 총 개수가 올바르다")
        void findByUserId_respectsPageSize() {
            saveMember(party1, user1, "개발자");
            saveMember(party2, user1, "기획자");

            Page<PartyMember> page = partyMemberRepository
                    .findByUserId(user1.getId(), PageRequest.of(0, 1));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }

    // ──────────────────────────────────────────
    // findRandomPartyMemberByPartyId() — LIMIT 1 네이티브 쿼리
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findRandomPartyMemberByPartyId()")
    class FindRandomPartyMemberByPartyId {

        @Test
        @DisplayName("파티에 멤버가 있으면 1명을 반환한다")
        void findRandomPartyMemberByPartyId_returnsMember() {
            saveMember(party1, user1, "개발자");
            saveMember(party1, user2, "디자이너");

            Optional<PartyMember> result = partyMemberRepository
                    .findRandomPartyMemberByPartyId(party1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getParty().getId()).isEqualTo(party1.getId());
        }

        @Test
        @DisplayName("파티에 멤버가 없으면 빈 Optional을 반환한다")
        void findRandomPartyMemberByPartyId_noMembers_returnsEmpty() {
            Optional<PartyMember> result = partyMemberRepository
                    .findRandomPartyMemberByPartyId(party1.getId());

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // existsByPartyId() — 네이티브 COUNT > 0 쿼리
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("existsByPartyId()")
    class ExistsByPartyId {

        @Test
        @DisplayName("파티에 멤버가 있으면 true를 반환한다")
        void existsByPartyId_hasMember_returnsTrue() {
            saveMember(party1, user1, "개발자");

            boolean result = partyMemberRepository.existsByPartyId(party1.getId());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("파티에 멤버가 없으면 false를 반환한다")
        void existsByPartyId_noMember_returnsFalse() {
            boolean result = partyMemberRepository.existsByPartyId(party1.getId());

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("다른 파티의 멤버는 영향을 주지 않는다")
        void existsByPartyId_memberInOtherParty_returnsFalse() {
            saveMember(party2, user1, "개발자"); // party2 에만 멤버

            boolean result = partyMemberRepository.existsByPartyId(party1.getId());

            assertThat(result).isFalse();
        }
    }
}
