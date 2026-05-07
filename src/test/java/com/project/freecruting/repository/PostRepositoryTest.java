package com.project.freecruting.repository;

import com.project.freecruting.model.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Post savePost(String title, String content, String author, Post.PostType type) {
        return postRepository.save(Post.builder()
                .title(title)
                .content(content)
                .author(author)
                .type(type)
                .author_id(1L)
                .build());
    }

    // ──────────────────────────────────────────
    // findAllDesc()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAllDesc()")
    class FindAllDesc {

        @Test
        @DisplayName("저장된 모든 게시글을 ID 내림차순으로 반환한다")
        void findAllDesc_returnsDescendingIdOrder() {
            savePost("제목1", "내용1", "작성자", Post.PostType.REVIEW);
            savePost("제목2", "내용2", "작성자", Post.PostType.STUDY);
            savePost("제목3", "내용3", "작성자", Post.PostType.PROJECT);

            List<Post> result = postRepository.findAllDesc();

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
            assertThat(result.get(1).getId()).isGreaterThan(result.get(2).getId());
        }
    }

    // ──────────────────────────────────────────
    // findByType()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByType()")
    class FindByType {

        @Test
        @DisplayName("지정한 타입의 게시글만 반환한다")
        void findByType_returnsOnlyMatchingType() {
            savePost("스터디1", "내용", "작성자", Post.PostType.STUDY);
            savePost("스터디2", "내용", "작성자", Post.PostType.STUDY);
            savePost("프로젝트", "내용", "작성자", Post.PostType.PROJECT);

            List<Post> result = postRepository.findByType(Post.PostType.STUDY);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(p -> p.getType() == Post.PostType.STUDY);
        }
    }

    // ──────────────────────────────────────────
    // findByTitle() — 제목 LIKE 검색
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByTitle()")
    class FindByTitle {

        @Test
        @DisplayName("제목에 키워드가 포함된 게시글만 반환한다")
        void findByTitle_containsKeyword() {
            savePost("Spring Boot 입문", "내용", "작성자", Post.PostType.STUDY);
            savePost("Spring Security 심화", "내용", "작성자", Post.PostType.STUDY);
            savePost("Java 기초", "내용", "작성자", Post.PostType.REVIEW);

            Page<Post> result = postRepository.findByTitle("Spring", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getTitle().contains("Spring"));
        }

        @Test
        @DisplayName("키워드에 매칭되는 제목이 없으면 빈 페이지를 반환한다")
        void findByTitle_noMatch_returnsEmpty() {
            savePost("Java 기초", "내용", "작성자", Post.PostType.REVIEW);

            Page<Post> result = postRepository.findByTitle("없는키워드", PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findByContent() — 내용 LIKE 검색
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByContent()")
    class FindByContent {

        @Test
        @DisplayName("내용에 키워드가 포함된 게시글만 반환한다")
        void findByContent_containsKeyword() {
            savePost("제목1", "Spring Boot 활용법", "작성자", Post.PostType.STUDY);
            savePost("제목2", "Java 기초 강의", "작성자", Post.PostType.REVIEW);

            Page<Post> result = postRepository.findByContent("Spring", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("제목1");
        }
    }

    // ──────────────────────────────────────────
    // findByAuthor() — 작성자 LIKE 검색
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByAuthor()")
    class FindByAuthor {

        @Test
        @DisplayName("작성자명에 키워드가 포함된 게시글만 반환한다")
        void findByAuthor_containsKeyword() {
            savePost("제목1", "내용", "김개발", Post.PostType.STUDY);
            savePost("제목2", "내용", "이테스트", Post.PostType.REVIEW);
            savePost("제목3", "내용", "김디자인", Post.PostType.PROJECT);

            Page<Post> result = postRepository.findByAuthor("김", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getAuthor().contains("김"));
        }
    }

    // ──────────────────────────────────────────
    // searchByKeyword() — 제목/내용/작성자 통합 검색
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("searchByKeyword()")
    class SearchByKeyword {

        @Test
        @DisplayName("제목, 내용, 작성자 중 하나라도 키워드가 포함되면 반환한다")
        void searchByKeyword_matchesAnyField() {
            savePost("Spring 입문", "내용A", "작성자A", Post.PostType.STUDY);         // 제목 매칭
            savePost("제목B", "Spring 강의 내용", "작성자B", Post.PostType.REVIEW);   // 내용 매칭
            savePost("제목C", "내용C", "Spring개발자", Post.PostType.PROJECT);        // 작성자 매칭
            savePost("제목D", "내용D", "작성자D", Post.PostType.ANNOUNCEMENT);        // 미매칭

            Page<Post> result = postRepository.searchByKeyword("Spring", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("키워드에 매칭되는 항목이 없으면 빈 페이지를 반환한다")
        void searchByKeyword_noMatch_returnsEmpty() {
            savePost("제목", "내용", "작성자", Post.PostType.STUDY);

            Page<Post> result = postRepository.searchByKeyword("없는키워드", PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findAllByOrderByIdDesc() — 페이징
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAllByOrderByIdDesc()")
    class FindAllByOrderByIdDesc {

        @Test
        @DisplayName("size만큼만 반환하고 총 개수/페이지 수가 올바르다")
        void findAllByOrderByIdDesc_respectsPageSize() {
            for (int i = 1; i <= 5; i++) {
                savePost("제목" + i, "내용" + i, "작성자", Post.PostType.REVIEW);
            }

            Page<Post> page = postRepository.findAllByOrderByIdDesc(PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("두 번째 페이지에서 나머지 게시글을 반환한다")
        void findAllByOrderByIdDesc_secondPage_returnsRemainder() {
            for (int i = 1; i <= 5; i++) {
                savePost("제목" + i, "내용" + i, "작성자", Post.PostType.REVIEW);
            }

            Page<Post> page = postRepository.findAllByOrderByIdDesc(PageRequest.of(1, 3));

            assertThat(page.getContent()).hasSize(2);
        }
    }

    // ──────────────────────────────────────────
    // findByTypeOrderByIdDesc() — 타입별 페이징
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByTypeOrderByIdDesc()")
    class FindByTypeOrderByIdDesc {

        @Test
        @DisplayName("해당 타입의 게시글만 페이징하여 반환한다")
        void findByTypeOrderByIdDesc_filtersByType() {
            savePost("스터디1", "내용", "작성자", Post.PostType.STUDY);
            savePost("스터디2", "내용", "작성자", Post.PostType.STUDY);
            savePost("프로젝트", "내용", "작성자", Post.PostType.PROJECT);

            Page<Post> page = postRepository.findByTypeOrderByIdDesc(PageRequest.of(0, 10), Post.PostType.STUDY);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allMatch(p -> p.getType() == Post.PostType.STUDY);
        }
    }

    // ──────────────────────────────────────────
    // increaseViews() — 조회수 증가
    // flush + clear로 1차 캐시 무효화 후 검증
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("increaseViews()")
    class IncreaseViews {

        @Test
        @DisplayName("increaseViews(id) 호출 시 조회수가 1 증가한다")
        void increaseViews_incrementsByOne() {
            Post post = savePost("제목", "내용", "작성자", Post.PostType.REVIEW);
            entityManager.flush();

            postRepository.increaseViews(post.getId());
            entityManager.clear(); // 1차 캐시 초기화 → DB에서 재조회

            Post updated = postRepository.findById(post.getId()).get();
            assertThat(updated.getViews()).isEqualTo(1);
        }

        @Test
        @DisplayName("increaseViews(id, n) 호출 시 조회수가 n만큼 증가한다")
        void increaseViews_incrementsByN() {
            Post post = savePost("제목", "내용", "작성자", Post.PostType.REVIEW);
            entityManager.flush();

            postRepository.increaseViews(post.getId(), 10L);
            entityManager.clear();

            Post updated = postRepository.findById(post.getId()).get();
            assertThat(updated.getViews()).isEqualTo(10);
        }
    }
}
