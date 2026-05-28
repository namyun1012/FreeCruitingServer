package com.project.freecruting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.handler.GlobalExceptionHandler;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.User;
import com.project.freecruting.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.springframework.data.domain.PageRequest;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @InjectMocks
    private PostController postController;

    @Mock
    private PostService postService;

    private MockMvc mockMvc;          // 로그인 사용자
    private MockMvc anonymousMockMvc; // 비로그인 사용자

    private SessionUser sessionUser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // SessionUser 생성을 위해 User를 Mock으로 처리
        // (@Mock 필드가 아니므로 strict stubbing 검사 대상 아님)
        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(USER_ID);
        given(mockUser.getName()).willReturn("테스트유저");
        given(mockUser.getEmail()).willReturn("test@test.com");
        given(mockUser.getPicture()).willReturn("pic.jpg");
        sessionUser = new SessionUser(mockUser);

        mockMvc = buildMockMvc(sessionUser);
        anonymousMockMvc = buildMockMvc(null);
    }

    /**
     * @LoginUser 파라미터에 원하는 SessionUser를 주입하는 테스트용 MockMvc.
     * standaloneSetup을 사용해 Spring 전체 컨텍스트(DB, Redis) 없이 실행.
     */
    private MockMvc buildMockMvc(SessionUser user) {
        return MockMvcBuilders.standaloneSetup(postController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterAnnotation(LoginUser.class) != null
                                && SessionUser.class.equals(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return user;
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PostResponseDto buildPostResponseDto(Long id) {
        Post post = Post.builder()
                .title("제목").content("내용").author("작성자")
                .type(Post.PostType.REVIEW).author_id(USER_ID).build();
        ReflectionTestUtils.setField(post, "id", id);
        return new PostResponseDto(post);
    }

    // ──────────────────────────────────────────
    // POST /api/v1/posts
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/posts")
    class Save {

        @Test
        @DisplayName("로그인 사용자가 요청하면 200 OK와 게시글 ID를 반환한다")
        void save_loggedIn_returns200() throws Exception {
            PostSaveRequestDto dto = PostSaveRequestDto.builder()
                    .title("제목").content("내용").author("작성자").type("REVIEW").build();
            given(postService.save(any(PostSaveRequestDto.class), eq(USER_ID))).willReturn(1L);

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }
    }

    // ──────────────────────────────────────────
    // PUT /api/v1/posts/{id}
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/v1/posts/{id}")
    class Update {

        private PostUpdateRequestDto dto;

        @BeforeEach
        void setUp() {
            dto = PostUpdateRequestDto.builder()
                    .title("수정된 제목").content("수정된 내용").type("STUDY").build();
        }

        @Test
        @DisplayName("작성자가 수정하면 200 OK와 성공 메시지를 반환한다")
        void update_byAuthor_returns200() throws Exception {
            given(postService.update(eq(1L), any(PostUpdateRequestDto.class), eq(USER_ID))).willReturn(1L);

            mockMvc.perform(put("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Update successful"));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 수정 요청 시 404를 반환한다")
        void update_notFound_returns404() throws Exception {
            given(postService.update(eq(999L), any(PostUpdateRequestDto.class), eq(USER_ID)))
                    .willThrow(new NotFoundException("게시글 없음"));

            mockMvc.perform(put("/api/v1/posts/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("작성자가 아닌 사용자의 수정 요청 시 403을 반환한다")
        void update_notAuthor_returns403() throws Exception {
            given(postService.update(eq(1L), any(PostUpdateRequestDto.class), eq(USER_ID)))
                    .willThrow(new ForbiddenException("권한 없음"));

            mockMvc.perform(put("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────────
    // GET /api/v1/posts/{id}
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/posts/{id}")
    class FindById {

        @Test
        @DisplayName("로그인 사용자가 조회하면 200 OK와 게시글 정보를 반환한다")
        void findById_loggedIn_returns200() throws Exception {
            given(postService.findById(eq(1L), eq("u:" + USER_ID))).willReturn(buildPostResponseDto(1L));

            mockMvc.perform(get("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("제목"));
        }

        @Test
        @DisplayName("비로그인 사용자도 조회할 수 있다 (200 OK, 세션 ID 기반 identifier 사용)")
        void findById_anonymous_returns200() throws Exception {
            // 비로그인 시 컨트롤러는 "s:{sessionId}" 형식의 identifier를 전달
            given(postService.findById(eq(1L), anyString())).willReturn(buildPostResponseDto(1L));

            anonymousMockMvc.perform(get("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 조회 시 404를 반환한다")
        void findById_notFound_returns404() throws Exception {
            given(postService.findById(eq(999L), eq("u:" + USER_ID)))
                    .willThrow(new NotFoundException("게시글 없음"));

            mockMvc.perform(get("/api/v1/posts/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ──────────────────────────────────────────
    // DELETE /api/v1/posts/{id}
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/v1/posts/{id}")
    class Delete {

        @Test
        @DisplayName("작성자가 삭제하면 200 OK와 성공 메시지를 반환한다")
        void delete_byAuthor_returns200() throws Exception {
            given(postService.delete(eq(1L), eq(USER_ID))).willReturn(1L);

            mockMvc.perform(delete("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Delete successful"));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 삭제 요청 시 404를 반환한다")
        void delete_notFound_returns404() throws Exception {
            given(postService.delete(eq(999L), eq(USER_ID)))
                    .willThrow(new NotFoundException("게시글 없음"));

            mockMvc.perform(delete("/api/v1/posts/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("작성자가 아닌 사용자의 삭제 요청 시 403을 반환한다")
        void delete_notAuthor_returns403() throws Exception {
            given(postService.delete(eq(1L), eq(USER_ID)))
                    .willThrow(new ForbiddenException("권한 없음"));

            mockMvc.perform(delete("/api/v1/posts/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────────
    // GET /api/v1/posts
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/posts")
    class GetPosts {

        private final Page<PostListResponseDto> emptyPage =
                new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        @Test
        @DisplayName("파라미터 없이 요청하면 200 OK와 전체 목록을 반환한다")
        void getPosts_noParams_returns200() throws Exception {
            given(postService.findPostPages(isNull(), isNull(), isNull(), eq(0), eq(10)))
                    .willReturn(emptyPage);

            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("type 파라미터로 필터링하면 200 OK와 해당 목록을 반환한다")
        void getPosts_withType_returns200() throws Exception {
            given(postService.findPostPages(eq("STUDY"), isNull(), isNull(), eq(0), eq(10)))
                    .willReturn(emptyPage);

            mockMvc.perform(get("/api/v1/posts").param("type", "STUDY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("query + search_type 파라미터로 검색하면 200 OK와 검색 결과를 반환한다")
        void getPosts_withQuery_returns200() throws Exception {
            given(postService.findPostPages(isNull(), eq("스프링"), eq("title"), eq(0), eq(10)))
                    .willReturn(emptyPage);

            mockMvc.perform(get("/api/v1/posts")
                            .param("query", "스프링")
                            .param("search_type", "title"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}