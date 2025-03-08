package com.project.freecruting.service;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.SearchType;
import com.project.freecruting.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;

    @Transactional
    public Long save(PostSaveRequestDto requestDto, Long author_id) {
        requestDto.setAuthor_id(author_id);
        return postRepository.save(requestDto.toEntity()).getId();
    }

    @Transactional
    public Long update(Long id, PostUpdateRequestDto requestDto, Long author_id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음. id=" + id));
        Long post_author_id = post.getAuthor_id();

        if (!post_author_id.equals(author_id)) {
            return 0L;
        }

        post.update(requestDto.getTitle(), requestDto.getContent(), requestDto.getImageURL(), Post.PostType.valueOf(requestDto.getType()));
        return id;
    }

    @Transactional
    public Long delete(Long id, Long author_id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음. id = " + id));

        Long post_author_id = post.getAuthor_id();

        if (!post_author_id.equals(author_id)) {
            return 0L;
        }

        postRepository.delete(post);
        return id;
    }

    @Transactional
    public PostResponseDto findById(Long id) {
        Post entity = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음. id=" + id));
        postRepository.increaseViews(id);
        return new PostResponseDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> findAllDesc() {
        return postRepository.findAllDesc().stream()
                .map(PostListResponseDto:: new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> findAllPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return postRepository.findAllByOrderByModifiedDateDesc(pageable)
                .map(PostListResponseDto::new);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> findByType(String type, int page, int size) {
        Post.PostType postType = Post.PostType.fromString(type);
        Pageable pageable = PageRequest.of(page, size);


        return postRepository.findByTypeOrderByModifiedDateDesc(pageable, postType)
                .map(PostListResponseDto:: new);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> search(String query, SearchType searchType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostListResponseDto> result;

        if(searchType == SearchType.ALL) {
            result = postRepository.findByTitleOrContentOrAuthor(query, query, query, pageable).map(PostListResponseDto::new);
        }

        else if(searchType == SearchType.TITLE) {
            result = postRepository.findByTitle(query, pageable).map(PostListResponseDto::new);
        }

        else if(searchType == SearchType.CONTENT) {
            result = postRepository.findByContent(query, pageable).map(PostListResponseDto::new);
        }

        else if(searchType == SearchType.AUTHOR) {
            result = postRepository.findByAuthor(query, pageable).map(PostListResponseDto::new);
        }
        
        // 이상한 값일 때  검색
        else {
            result = postRepository.findByTitleOrContentOrAuthor(query, query, query, pageable).map(PostListResponseDto::new);
        }

        return result;
    }
}
