package com.project.freecruting.repository;

import com.project.freecruting.model.Party;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PartyRepositoryTest {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Party saveParty(String name, Long ownerId) {
        return partyRepository.save(Party.builder()
                .name(name)
                .description("설명")
                .owner_id(ownerId)
                .max_number(10)
                .build());
    }

    // ──────────────────────────────────────────
    // findAll(Pageable) — 파티 목록 페이징
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAll(Pageable)")
    class FindAllPageable {

        @Test
        @DisplayName("size만큼만 반환하고 총 개수/페이지 수가 올바르다")
        void findAll_respectsPageSize() {
            for (int i = 1; i <= 5; i++) {
                saveParty("파티" + i, (long) i);
            }

            Page<Party> page = partyRepository.findAll(PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("두 번째 페이지에서 나머지 파티를 반환한다")
        void findAll_secondPage_returnsRemainder() {
            for (int i = 1; i <= 5; i++) {
                saveParty("파티" + i, (long) i);
            }

            Page<Party> page = partyRepository.findAll(PageRequest.of(1, 3));

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("파티가 없으면 빈 페이지를 반환한다")
        void findAll_noParty_returnsEmptyPage() {
            Page<Party> page = partyRepository.findAll(PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────
    // updateOwnerIdById() — 파티 소유자 변경
    // flush + clear 로 1차 캐시 무효화 후 검증
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("updateOwnerIdById()")
    class UpdateOwnerIdById {

        @Test
        @DisplayName("updateOwnerIdById() 호출 시 owner_id가 변경된다")
        void updateOwnerIdById_changesOwner() {
            Party party = saveParty("파티", 1L);
            entityManager.flush();

            int updated = partyRepository.updateOwnerIdById(2L, party.getId());
            entityManager.clear();

            Party result = partyRepository.findById(party.getId()).get();
            assertThat(updated).isEqualTo(1);
            assertThat(result.getOwner_id()).isEqualTo(2L);
        }

        @Test
        @DisplayName("존재하지 않는 파티 ID에 대해서는 영향받은 행이 0이다")
        void updateOwnerIdById_nonExistentId_returnsZero() {
            int updated = partyRepository.updateOwnerIdById(2L, 9999L);

            assertThat(updated).isEqualTo(0);
        }

        @Test
        @DisplayName("다른 파티의 owner_id는 변경되지 않는다")
        void updateOwnerIdById_doesNotAffectOtherParties() {
            Party party1 = saveParty("파티A", 1L);
            Party party2 = saveParty("파티B", 1L);
            entityManager.flush();

            partyRepository.updateOwnerIdById(99L, party1.getId());
            entityManager.clear();

            Party unchanged = partyRepository.findById(party2.getId()).get();
            assertThat(unchanged.getOwner_id()).isEqualTo(1L);
        }
    }
}
