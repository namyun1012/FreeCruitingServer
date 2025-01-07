package com.project.freecruting.service;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;

    @Transactional
    public Long save(PostSaveRequestDto requestDto) {
        return postRepository.save(requestDto.toEntity()).getId();
    }

    @Transactional
    public Long update(Long id, PostUpdateRequestDto requestDto) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음. id=" + id));
        post.update(requestDto.getTitle(), requestDto.getContent(), requestDto.getImageURL(), requestDto.getType());
        return id;
    }

    public PostResponseDto findById(Long id) {
        Post entity = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음. id=" + id));
        return new PostResponseDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> findAllDesc() {
        return postRepository.findAllDesc().stream()
                .map(PostListResponseDto:: new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> findByType(String type) {
        return postRepository.findByType(type).stream()
                .map(PostListResponseDto:: new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음. id = " + id));
        postRepository.delete(post);
    }
}
