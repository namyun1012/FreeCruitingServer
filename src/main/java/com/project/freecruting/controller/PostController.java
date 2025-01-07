package com.project.freecruting.controller;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class PostController {

    private final PostService postService;
    // Save 용도
    @PostMapping("/api/v1/post")
    public Long save(@RequestBody PostSaveRequestDto requestDto) {
        return postService.save(requestDto);
    }

    @PutMapping("/api/v1/post/{id}")
    public Long update(@PathVariable Long id, @RequestBody PostUpdateRequestDto requestDto) {
        return postService.update(id, requestDto);
    }

    @GetMapping("/api/v1/post/{id}")
    public PostResponseDto findById(@PathVariable Long id) {
        return postService.findById(id);
    }

    @DeleteMapping("/api/v1/post/{id}")
    public Long delete(@PathVariable Long id) {
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
