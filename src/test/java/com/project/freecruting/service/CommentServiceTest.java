package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentSaveRequestDto;
import com.project.freecruting.dto.comment.CommentUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.Role;
import com.project.freecruting.repository.CommentRepository;
import com.project.freecruting.repository.PostRepository;
import com.project.freecruting.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User createUser(Long id, String name) {
        User user = User.builder()
                .email("test@example.com")
                .name(name)
                .password("encoded")
                .role(Role.USER)
                .provider("local")
                .picture("favicon.ico")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post createPost(Long id, Long authorId) {
        Post post = Post.builder()
                .title("제목").content("내용").author("작성자")
                .type(Post.PostType.PROJECT).author_id(authorId).build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Comment createComment(Long id, Post post, User user) {
        Comment comment = Comment.builder()
                .content("댓글 내용").post(post).user(user).author(user.getName()).build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    // ──────────────────────────────────────────
    // getCommentsByPostId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("getCommentsByPostId()")
    class GetCommentsByPostId {

        @Test
        @DisplayName("게시글 ID로 댓글 목록을 반환한다")
        void getCommentsByPostId_returnsList() {
            Post post = createPost(1L, 10L);
            User user = createUser(10L, "작성자");
            Comment comment = createComment(1L, post, user);
            given(commentRepository.findByPostId(1L)).willReturn(List.of(comment));

            var result = commentService.getCommentsByPostId(1L);

            assertThat(result).hasSize(1);
            verify(commentRepository).findByPostId(1L);
        }

        @Test
        @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
        void getCommentsByPostId_noComments_returnsEmptyList() {
            given(commentRepository.findByPostId(1L)).willReturn(Collections.emptyList());

            var result = commentService.getCommentsByPostId(1L);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findAllPageByPostId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findAllPageByPostId()")
    class FindAllPageByPostId {

        @Test
        @DisplayName("페이징된 댓글 목록을 반환한다")
        void findAllPageByPostId_returnsPage() {
            Post post = createPost(1L, 10L);
            User user = createUser(10L, "작성자");
            Comment comment = createComment(1L, post, user);
            Page<Comment> page = new PageImpl<>(List.of(comment));
            given(commentRepository.findAllByPostIdOrderByModifiedDateDesc(eq(1L), any(Pageable.class)))
                    .willReturn(page);

            var result = commentService.findAllPageByPostId(0, 10, 1L);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // ──────────────────────────────────────────
    // save()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("저장 성공 시 댓글 ID를 반환하고 이벤트를 발행한다")
        void save_success_returnsIdAndPublishesEvent() {
            Post post = createPost(1L, 20L);
            User user = createUser(10L, "댓글작성자");
            Comment savedComment = createComment(5L, post, user);

            CommentSaveRequestDto dto = CommentSaveRequestDto.builder()
                    .content("댓글 내용").post_id(1L).build();

            given(postRepository.findById(1L)).willReturn(Optional.of(post));
            given(userRepository.findById(10L)).willReturn(Optional.of(user));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

            Long result = commentService.save(dto, 10L);

            assertThat(result).isEqualTo(5L);
            // CommentCreatedEvent는 ApplicationEvent 미상속 → publishEvent(Object) 오버로드 호출
            verify(eventPublisher).publishEvent((Object) any());
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 NotFoundException 발생")
        void save_postNotFound_throwsNotFoundException() {
            given(postRepository.findById(anyLong())).willReturn(Optional.empty());
            CommentSaveRequestDto dto = CommentSaveRequestDto.builder()
                    .content("내용").post_id(999L).build();

            assertThatThrownBy(() -> commentService.save(dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 유저이면 NotFoundException 발생")
        void save_userNotFound_throwsNotFoundException() {
            Post post = createPost(1L, 20L);
            given(postRepository.findById(1L)).willReturn(Optional.of(post));
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            CommentSaveRequestDto dto = CommentSaveRequestDto.builder()
                    .content("내용").post_id(1L).build();

            assertThatThrownBy(() -> commentService.save(dto, 999L))
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
        @DisplayName("작성자가 수정하면 내용이 변경되고 ID를 반환한다")
        void update_byAuthor_success() {
            Post post = createPost(1L, 20L);
            User user = createUser(10L, "작성자");
            Comment comment = createComment(1L, post, user);
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            CommentUpdateRequestDto dto = CommentUpdateRequestDto.builder().content("수정된 내용").build();
            Long result = commentService.update(1L, dto, 10L);

            assertThat(result).isEqualTo(1L);
            assertThat(comment.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 NotFoundException 발생")
        void update_notFound_throwsNotFoundException() {
            given(commentRepository.findById(anyLong())).willReturn(Optional.empty());
            CommentUpdateRequestDto dto = CommentUpdateRequestDto.builder().content("내용").build();

            assertThatThrownBy(() -> commentService.update(999L, dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정하면 ForbiddenException 발생")
        void update_notAuthor_throwsForbiddenException() {
            Post post = createPost(1L, 20L);
            User user = createUser(10L, "작성자");
            Comment comment = createComment(1L, post, user);
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            CommentUpdateRequestDto dto = CommentUpdateRequestDto.builder().content("내용").build();

            assertThatThrownBy(() -> commentService.update(1L, dto, 99L))
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
        @DisplayName("작성자가 삭제하면 delete()가 호출되고 ID를 반환한다")
        void delete_byAuthor_success() {
            Post post = createPost(1L, 20L);
            User user = createUser(10L, "작성자");
            Comment comment = createComment(1L, post, user);
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            Long result = commentService.delete(1L, 10L);

            assertThat(result).isEqualTo(1L);
            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 NotFoundException 발생")
        void delete_notFound_throwsNotFoundException() {
            given(commentRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.delete(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 삭제하면 ForbiddenException 발생")
        void delete_notAuthor_throwsForbiddenException() {
            Post post = createPost(1L, 20L);
            User user = createUser(10L, "작성자");
            Comment comment = createComment(1L, post, user);
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.delete(1L, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
