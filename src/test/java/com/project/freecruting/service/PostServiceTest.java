package com.project.freecruting.service;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.type.SearchType;
import com.project.freecruting.repository.CommentRepository;
import com.project.freecruting.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    // id는 JPA가 채워주는 필드라 Builder로 못 세팅 → ReflectionTestUtils 사용
    private Post createPost(Long id, Long authorId) {
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .author("작성자")
                .imageURL("image.jpg")
                .type(Post.PostType.REVIEW)
                .author_id(authorId)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    // ──────────────────────────────────────────
    // save()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("저장 성공 시 게시글 ID를 반환한다")
        void save_success_returnsId() {
            PostSaveRequestDto dto = PostSaveRequestDto.builder()
                    .title("제목").content("내용").author("작성자").type("REVIEW").build();
            Post savedPost = createPost(1L, 10L);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            Long result = postService.save(dto, 10L);

            assertThat(result).isEqualTo(1L);
            verify(postRepository).save(any(Post.class));
        }
    }

    // ──────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("작성자가 수정하면 변경 내용이 반영되고 ID를 반환한다")
        void update_byAuthor_success() {
            Long postId = 1L, authorId = 10L;
            Post post = createPost(postId, authorId);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            PostUpdateRequestDto dto = PostUpdateRequestDto.builder()
                    .title("수정된 제목").content("수정된 내용").type("STUDY").build();

            Long result = postService.update(postId, dto, authorId);

            assertThat(result).isEqualTo(postId);
            assertThat(post.getTitle()).isEqualTo("수정된 제목");
            assertThat(post.getContent()).isEqualTo("수정된 내용");
            assertThat(post.getType()).isEqualTo(Post.PostType.STUDY);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 NotFoundException 발생")
        void update_notFound_throwsNotFoundException() {
            given(postRepository.findById(anyLong())).willReturn(Optional.empty());
            PostUpdateRequestDto dto = PostUpdateRequestDto.builder()
                    .title("제목").content("내용").type("REVIEW").build();

            assertThatThrownBy(() -> postService.update(999L, dto, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정하면 ForbiddenException 발생")
        void update_notAuthor_throwsForbiddenException() {
            Long postId = 1L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            PostUpdateRequestDto dto = PostUpdateRequestDto.builder()
                    .title("제목").content("내용").type("REVIEW").build();

            assertThatThrownBy(() -> postService.update(postId, dto, 99L))
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
            Long postId = 1L, authorId = 10L;
            Post post = createPost(postId, authorId);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            Long result = postService.delete(postId, authorId);

            assertThat(result).isEqualTo(postId);
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 NotFoundException 발생")
        void delete_notFound_throwsNotFoundException() {
            given(postRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.delete(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 삭제하면 ForbiddenException 발생")
        void delete_notAuthor_throwsForbiddenException() {
            Long postId = 1L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.delete(postId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ──────────────────────────────────────────
    // findById() — 조회수 처리 분기
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Redis 미사용 시 DB 조회수 증가(increaseViews)를 호출한다")
        void findById_redisDisabled_incrementsDb() {
            ReflectionTestUtils.setField(postService, "useRedis", false);
            Long postId = 1L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            PostResponseDto result = postService.findById(postId, 1L);

            verify(postRepository).increaseViews(postId);
            assertThat(result.getId()).isEqualTo(postId);
        }

        @Test
        @DisplayName("Redis 활성화여도 비로그인(userId=null)이면 DB 조회수 증가를 호출한다")
        void findById_redisEnabled_nullUser_incrementsDb() {
            ReflectionTestUtils.setField(postService, "useRedis", true);
            Long postId = 1L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            postService.findById(postId, null);

            verify(postRepository).increaseViews(postId);
            verify(redisTemplate, never()).opsForSet();
        }

        @Test
        @DisplayName("Redis 사용, 첫 조회이면 Redis 조회수를 증가시키고 DB는 건드리지 않는다")
        void findById_redisEnabled_firstView_incrementsRedis() {
            ReflectionTestUtils.setField(postService, "useRedis", true);
            Long postId = 1L, userId = 5L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add(anyString(), anyString())).willReturn(1L); // 신규 사용자
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            postService.findById(postId, userId);

            verify(valueOperations).increment(anyString());
            verify(postRepository, never()).increaseViews(postId);
        }

        @Test
        @DisplayName("Redis 사용, 24시간 내 재조회이면 조회수를 올리지 않는다")
        void findById_redisEnabled_duplicateView_skipsIncrement() {
            ReflectionTestUtils.setField(postService, "useRedis", true);
            Long postId = 1L, userId = 5L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add(anyString(), anyString())).willReturn(0L); // 이미 조회한 사용자

            postService.findById(postId, userId);

            verify(redisTemplate, never()).opsForValue();
            verify(postRepository, never()).increaseViews(postId);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 NotFoundException 발생")
        void findById_notFound_throwsNotFoundException() {
            given(postRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.findById(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // findByIdForUpdate()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByIdForUpdate()")
    class FindByIdForUpdate {

        @Test
        @DisplayName("작성자가 조회하면 PostResponseDto를 반환한다")
        void findByIdForUpdate_author_success() {
            Long postId = 1L, authorId = 10L;
            Post post = createPost(postId, authorId);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            PostResponseDto result = postService.findByIdForUpdate(postId, authorId);

            assertThat(result.getId()).isEqualTo(postId);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 NotFoundException 발생")
        void findByIdForUpdate_notFound_throwsNotFoundException() {
            given(postRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.findByIdForUpdate(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아니면 ForbiddenException 발생")
        void findByIdForUpdate_notAuthor_throwsForbiddenException() {
            Long postId = 1L;
            Post post = createPost(postId, 10L);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.findByIdForUpdate(postId, 99L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ──────────────────────────────────────────
    // findPostPages() — 파라미터에 따른 분기 검증
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findPostPages()")
    class FindPostPages {

        private final Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());

        @Test
        @DisplayName("query + search_type이 있으면 search()를 호출한다")
        void findPostPages_withQueryAndSearchType_callsSearch() {
            given(postRepository.searchByKeyword(anyString(), any(Pageable.class))).willReturn(emptyPage);

            postService.findPostPages(null, "스프링", "all", 0, 10);

            verify(postRepository).searchByKeyword(eq("스프링"), any(Pageable.class));
            verify(postRepository, never()).findAllByOrderByIdDesc(any());
        }

        @Test
        @DisplayName("type만 있으면 findByType()을 호출한다")
        void findPostPages_withTypeOnly_callsFindByType() {
            given(postRepository.findByTypeOrderByIdDesc(any(Pageable.class), eq(Post.PostType.STUDY)))
                    .willReturn(emptyPage);

            postService.findPostPages("STUDY", null, null, 0, 10);

            verify(postRepository).findByTypeOrderByIdDesc(any(Pageable.class), eq(Post.PostType.STUDY));
            verify(postRepository, never()).findAllByOrderByIdDesc(any());
        }

        @Test
        @DisplayName("파라미터가 없으면 전체 목록 조회(findAllPage)를 호출한다")
        void findPostPages_noParams_callsFindAll() {
            given(postRepository.findAllByOrderByIdDesc(any(Pageable.class))).willReturn(emptyPage);

            postService.findPostPages(null, null, null, 0, 10);

            verify(postRepository).findAllByOrderByIdDesc(any(Pageable.class));
        }
    }

    // ──────────────────────────────────────────
    // search() — SearchType별 Repository 메서드 라우팅
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("search()")
    class Search {

        private final Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());

        @Test
        @DisplayName("SearchType.TITLE이면 findByTitle()을 호출한다")
        void search_titleType_callsFindByTitle() {
            given(postRepository.findByTitle(anyString(), any(Pageable.class))).willReturn(emptyPage);

            postService.search("검색어", SearchType.TITLE, 0, 10);

            verify(postRepository).findByTitle(eq("검색어"), any(Pageable.class));
        }

        @Test
        @DisplayName("SearchType.CONTENT이면 findByContent()을 호출한다")
        void search_contentType_callsFindByContent() {
            given(postRepository.findByContent(anyString(), any(Pageable.class))).willReturn(emptyPage);

            postService.search("검색어", SearchType.CONTENT, 0, 10);

            verify(postRepository).findByContent(eq("검색어"), any(Pageable.class));
        }

        @Test
        @DisplayName("SearchType.AUTHOR이면 findByAuthor()을 호출한다")
        void search_authorType_callsFindByAuthor() {
            given(postRepository.findByAuthor(anyString(), any(Pageable.class))).willReturn(emptyPage);

            postService.search("작성자명", SearchType.AUTHOR, 0, 10);

            verify(postRepository).findByAuthor(eq("작성자명"), any(Pageable.class));
        }

        @Test
        @DisplayName("SearchType.ALL이면 searchByKeyword()를 호출한다")
        void search_allType_callsSearchByKeyword() {
            given(postRepository.searchByKeyword(anyString(), any(Pageable.class))).willReturn(emptyPage);

            postService.search("검색어", SearchType.ALL, 0, 10);

            verify(postRepository).searchByKeyword(eq("검색어"), any(Pageable.class));
        }
    }

    // ──────────────────────────────────────────
    // getCommentCountsByPostIds()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("getCommentCountsByPostIds()")
    class GetCommentCounts {

        @Test
        @DisplayName("빈 리스트 입력 시 Repository를 호출하지 않고 빈 Map을 반환한다")
        void emptyList_returnsEmptyMapWithoutDbCall() {
            Map<Long, Long> result = postService.getCommentCountsByPostIds(Collections.emptyList());

            assertThat(result).isEmpty();
            verify(commentRepository, never()).findCommentCountsByPostIds(any());
        }

        @Test
        @DisplayName("게시글 ID 리스트로 올바른 댓글 수 Map을 반환한다")
        void withIds_returnsCommentCountMap() {
            given(commentRepository.findCommentCountsByPostIds(List.of(1L, 2L)))
                    .willReturn(List.of(new Object[]{1L, 3L}, new Object[]{2L, 5L}));

            Map<Long, Long> result = postService.getCommentCountsByPostIds(List.of(1L, 2L));

            assertThat(result)
                    .hasSize(2)
                    .containsEntry(1L, 3L)
                    .containsEntry(2L, 5L);
        }
    }
}