package com.project.freecruting.controller;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;
    // Save 용도
    @PostMapping("/post")
    public Long save(@RequestBody PostSaveRequestDto requestDto, @AuthenticationPrincipal OAuth2User oAuth2User) {
        String author_id = oAuth2User.getAttribute("sub");
        return postService.save(requestDto, author_id);
    }

    @PutMapping("/post/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PostUpdateRequestDto requestDto, @AuthenticationPrincipal OAuth2User oAuth2User) {
        String author_id = oAuth2User.getAttribute("sub");

        Long result = postService.update(id, requestDto, author_id);

        if(result == 0L) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "작성자만 수정할 수 있습니다."));
        }

        return ResponseEntity.ok(Map.of("message", "Update successful"));
    }

    @GetMapping("/post/{id}")
    public PostResponseDto findById(@PathVariable Long id) {
        return postService.findById(id);
    }

    @DeleteMapping("/post/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User) {
        String author_id = oAuth2User.getAttribute("sub");
        Long result = postService.delete(id, author_id);

        if(result == 0L) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "작성자만 삭제할 수 있습니다."));
        }
        return ResponseEntity.ok(Map.of("message", "Delete successful"));
    }

    @GetMapping("/posts")
    public List<PostListResponseDto> getAllPosts() {
        return postService.findAllDesc();
    }

    // type 별 Post 받아오기, 아직 미 사용 중 추후에 에러 체크할 것
    @GetMapping("/posts/type")
    public List<PostListResponseDto> getPostsByType(@RequestParam(required = false) String type) {
        return postService.findByType(type);
    }
}
