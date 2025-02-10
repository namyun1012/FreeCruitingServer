package com.project.freecruting.controller;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class PostController {

    private final PostService postService;
    // Save 용도
    @PostMapping("/api/v1/post")
    public Long save(@RequestBody PostSaveRequestDto requestDto, @AuthenticationPrincipal OAuth2User oAuth2User) {
        String author_id = oAuth2User.getAttribute("sub");
        return postService.save(requestDto, author_id);
    }

    @PutMapping("/api/v1/post/{id}")
    public Long update(@PathVariable Long id, @RequestBody PostUpdateRequestDto requestDto, @AuthenticationPrincipal OAuth2User oAuth2User) {
        String author_id = oAuth2User.getAttribute("sub");
        return postService.update(id, requestDto);
    }

    @GetMapping("/api/v1/post/{id}")
    public PostResponseDto findById(@PathVariable Long id) {
        return postService.findById(id);
    }

    @DeleteMapping("/api/v1/post/{id}")
    public Long delete(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User) {
        String author_id = oAuth2User.getAttribute("sub");
        postService.delete(id);
        return id;
    }

    @GetMapping("/api/v1/posts")
    public List<PostListResponseDto> getAllPosts() {
        return postService.findAllDesc();
    }
    
    // 추후에 에러 체크할 것
    @GetMapping("/api/v1/posts/type")
    public List<PostListResponseDto> getPostsByType(@RequestParam(required = false) String type) {
        return postService.findByType(type);
    }
}
