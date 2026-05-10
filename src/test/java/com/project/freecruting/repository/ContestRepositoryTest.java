package com.project.freecruting.repository;

import com.project.freecruting.model.Contest;
import com.project.freecruting.model.type.ContestCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ContestRepositoryTest {

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final LocalDate TODAY    = LocalDate.now();
    private final LocalDate DEADLINE = TODAY.plusDays(30);

    private Contest saveContest(String title, String organizer, String description, ContestCategory category) {
        return contestRepository.save(Contest.builder()
                .title(title)
                .organizer(organizer)
                .description(description)
                .category(category)
                .applicationStartDate(TODAY)
                .applicationDeadline(DEADLINE)
                .build());
    }

    // ──────────────────────────────────────────
    // findAllByOrderByIdDesc()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAllByOrderByIdDesc()")
    class FindAllByOrderByIdDesc {

        @Test
        @DisplayName("size만큼만 반환하고 총 개수/페이지 수가 올바르다")
        void findAll_respectsPageSize() {
            for (int i = 1; i <= 5; i++) {
                saveContest("공모전" + i, "주최" + i, "설명" + i, ContestCategory.PROGRAMMING);
            }

            Page<Contest> page = contestRepository.findAllByOrderByIdDesc(PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("ID 내림차순으로 반환한다")
        void findAll_returnsDescendingIdOrder() {
            saveContest("공모전1", "주최1", "설명1", ContestCategory.DESIGN);
            saveContest("공모전2", "주최2", "설명2", ContestCategory.DESIGN);

            Page<Contest> page = contestRepository.findAllByOrderByIdDesc(PageRequest.of(0, 10));

            assertThat(page.getContent().get(0).getId())
                    .isGreaterThan(page.getContent().get(1).getId());
        }
    }

    // ──────────────────────────────────────────
    // findByCategoryOrderByIdDesc()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByCategoryOrderByIdDesc()")
    class FindByCategoryOrderByIdDesc {

        @Test
        @DisplayName("지정한 카테고리의 공모전만 반환한다")
        void findByCategory_returnsOnlyMatchingCategory() {
            saveContest("개발공모전1", "주최A", "설명", ContestCategory.PROGRAMMING);
            saveContest("개발공모전2", "주최B", "설명", ContestCategory.PROGRAMMING);
            saveContest("디자인공모전", "주최C", "설명", ContestCategory.DESIGN);

            Page<Contest> page = contestRepository.findByCategoryOrderByIdDesc(
                    ContestCategory.PROGRAMMING, PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allMatch(c -> c.getCategory() == ContestCategory.PROGRAMMING);
        }

        @Test
        @DisplayName("해당 카테고리가 없으면 빈 페이지를 반환한다")
        void findByCategory_noMatch_returnsEmpty() {
            saveContest("개발공모전", "주최A", "설명", ContestCategory.PROGRAMMING);

            Page<Contest> page = contestRepository.findByCategoryOrderByIdDesc(
                    ContestCategory.STARTUP, PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // searchByKeyword() — 제목/주최기관/설명 통합 검색
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("searchByKeyword()")
    class SearchByKeyword {

        @Test
        @DisplayName("제목, 주최기관, 설명 중 하나라도 키워드가 포함되면 반환한다")
        void searchByKeyword_matchesAnyField() {
            saveContest("AI 공모전",    "주최A",    "설명A",           ContestCategory.PROGRAMMING); // 제목 매칭
            saveContest("공모전B",      "삼성전자",  "설명B",           ContestCategory.DESIGN);      // 주최 매칭
            saveContest("공모전C",      "주최C",    "AI 기술 활용 대회", ContestCategory.STARTUP);    // 설명 매칭
            saveContest("관계없는 공모전", "주최D",  "설명D",           ContestCategory.ART);         // 미매칭

            Page<Contest> result = contestRepository.searchByKeyword("AI", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("키워드에 매칭되는 항목이 없으면 빈 페이지를 반환한다")
        void searchByKeyword_noMatch_returnsEmpty() {
            saveContest("공모전", "주최기관", "설명", ContestCategory.PROGRAMMING);

            Page<Contest> result = contestRepository.searchByKeyword("없는키워드", PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // increaseViews() — 조회수 증가
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("increaseViews()")
    class IncreaseViews {

        @Test
        @DisplayName("increaseViews(id) 호출 시 조회수가 1 증가한다")
        void increaseViews_incrementsByOne() {
            Contest contest = saveContest("공모전", "주최", "설명", ContestCategory.PROGRAMMING);
            entityManager.flush();

            contestRepository.increaseViews(contest.getId());
            entityManager.clear();

            Contest updated = contestRepository.findById(contest.getId()).get();
            assertThat(updated.getViews()).isEqualTo(1);
        }

        @Test
        @DisplayName("increaseViews(id, n) 호출 시 조회수가 n만큼 증가한다")
        void increaseViews_incrementsByN() {
            Contest contest = saveContest("공모전", "주최", "설명", ContestCategory.PROGRAMMING);
            entityManager.flush();

            contestRepository.increaseViews(contest.getId(), 15L);
            entityManager.clear();

            Contest updated = contestRepository.findById(contest.getId()).get();
            assertThat(updated.getViews()).isEqualTo(15);
        }
    }
}
