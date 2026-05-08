package com.project.freecruting.repository;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Post post1;
    private Post post2;
    private User user;

    @BeforeEach
    void setUp() {
        user = entityManager.persistAndFlush(User.builder()
                .name("테스터")
                .email("test@example.com")
                .role(Role.USER)
                .build());

        post1 = entityManager.persistAndFlush(Post.builder()
                .title("게시글1")
                .content("내용1")
                .author("테스터")
                .type(Post.PostType.STUDY)
                .author_id(user.getId())
                .build());

        post2 = entityManager.persistAndFlush(Post.builder()
                .title("게시글2")
                .content("내용2")
                .author("테스터")
                .type(Post.PostType.PROJECT)
                .author_id(user.getId())
                .build());
    }

    private Comment saveComment(Post post, String content) {
        return commentRepository.save(Comment.builder()
                .content(content)
                .author(user.getName())
                .post(post)
                .user(user)
                .build());
    }

    // ──────────────────────────────────────────
    // findByPostId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByPostId()")
    class FindByPostId {

        @Test
        @DisplayName("특정 게시글의 댓글만 반환한다")
        void findByPostId_returnsCommentsForPost() {
            saveComment(post1, "댓글1");
            saveComment(post1, "댓글2");
            saveComment(post2, "다른게시글댓글");

            List<Comment> result = commentRepository.findByPostId(post1.getId());

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(c -> c.getPost().getId().equals(post1.getId()));
        }

        @Test
        @DisplayName("댓글이 없는 게시글은 빈 리스트를 반환한다")
        void findByPostId_noComments_returnsEmpty() {
            List<Comment> result = commentRepository.findByPostId(post1.getId());

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findAllByPostIdOrderByModifiedDateDesc() — 최신 수정순 페이징
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAllByPostIdOrderByModifiedDateDesc()")
    class FindAllByPostIdOrderByModifiedDateDesc {

        @Test
        @DisplayName("페이지 크기만큼 반환하고 총 개수가 올바르다")
        void findAllByPostIdOrderByModifiedDateDesc_respectsPageSize() {
            for (int i = 1; i <= 5; i++) {
                saveComment(post1, "댓글" + i);
            }

            Page<Comment> page = commentRepository.findAllByPostIdOrderByModifiedDateDesc(
                    post1.getId(), PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("두 번째 페이지에서 나머지 댓글을 반환한다")
        void findAllByPostIdOrderByModifiedDateDesc_secondPage_returnsRemainder() {
            for (int i = 1; i <= 5; i++) {
                saveComment(post1, "댓글" + i);
            }

            Page<Comment> page = commentRepository.findAllByPostIdOrderByModifiedDateDesc(
                    post1.getId(), PageRequest.of(1, 3));

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("다른 게시글의 댓글은 페이징 결과에 포함되지 않는다")
        void findAllByPostIdOrderByModifiedDateDesc_filtersOtherPosts() {
            saveComment(post1, "post1 댓글");
            saveComment(post2, "post2 댓글");

            Page<Comment> page = commentRepository.findAllByPostIdOrderByModifiedDateDesc(
                    post1.getId(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getPost().getId()).isEqualTo(post1.getId());
        }
    }

    // ──────────────────────────────────────────
    // findCommentCountsByPostIds() — 게시글별 댓글 수 일괄 집계
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findCommentCountsByPostIds()")
    class FindCommentCountsByPostIds {

        @Test
        @DisplayName("각 게시글의 댓글 수를 올바르게 집계한다")
        void findCommentCountsByPostIds_aggregatesCorrectly() {
            saveComment(post1, "댓글A");
            saveComment(post1, "댓글B");
            saveComment(post2, "댓글C");

            List<Object[]> result = commentRepository.findCommentCountsByPostIds(
                    List.of(post1.getId(), post2.getId()));

            assertThat(result).hasSize(2);

            long countForPost1 = result.stream()
                    .filter(row -> row[0].equals(post1.getId()))
                    .mapToLong(row -> (Long) row[1])
                    .findFirst()
                    .orElse(0L);
            long countForPost2 = result.stream()
                    .filter(row -> row[0].equals(post2.getId()))
                    .mapToLong(row -> (Long) row[1])
                    .findFirst()
                    .orElse(0L);

            assertThat(countForPost1).isEqualTo(2);
            assertThat(countForPost2).isEqualTo(1);
        }

        @Test
        @DisplayName("댓글이 없는 게시글은 집계 결과에 포함되지 않는다")
        void findCommentCountsByPostIds_excludesPostsWithNoComments() {
            saveComment(post1, "댓글");

            List<Object[]> result = commentRepository.findCommentCountsByPostIds(
                    List.of(post1.getId(), post2.getId()));

            assertThat(result).hasSize(1);
            assertThat(result.get(0)[0]).isEqualTo(post1.getId());
        }

        @Test
        @DisplayName("빈 목록을 전달하면 빈 결과를 반환한다")
        void findCommentCountsByPostIds_emptyList_returnsEmpty() {
            List<Object[]> result = commentRepository.findCommentCountsByPostIds(List.of());

            assertThat(result).isEmpty();
        }
    }
}
