package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentListResponseDto;
import com.project.freecruting.dto.comment.CommentSaveRequestDto;
import com.project.freecruting.dto.comment.CommentUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.User;
import com.project.freecruting.repository.CommentRepository;
import com.project.freecruting.repository.PostRepository;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CommentListResponseDto> getCommentsByPostId(Long post_id) {
        return commentRepository.findByPostId(post_id).stream()
                .map(CommentListResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> findAllPageByPostId(int page, int size, Long post_id) {
        Pageable pageable = PageRequest.of(page, size);

        return commentRepository.findAllByPostIdOrderByModifiedDateDesc(post_id, pageable)
                .map(CommentListResponseDto::new);
    }

    @Transactional
    public Long save(CommentSaveRequestDto requestDto, Long user_id) {
        Long post_id = requestDto.getPost_id();
        Post post = postRepository.findById(post_id).orElseThrow(() -> new NotFoundException("해당 POST 없음"));
        User user = userRepository.findById(user_id).orElseThrow(() -> new NotFoundException("해당 USER 없음"));

        return commentRepository.save(requestDto.toEntity(post,user)).getId();
    }

    @Transactional
    public Long update(Long id, CommentUpdateRequestDto requestDto, Long author_id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 COMMENT 없음. id=" + id));
        Long comment_author_id = comment.getUser().getId();

        if (!comment_author_id.equals(author_id)) {
            throw new ForbiddenException("해당 댓글의 작성자 아님");
        }

        comment.update(requestDto.getContent());
        return id;
    }

    @Transactional
    public Long delete(Long id, Long author_id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 COMMENT 없음. id = " + id));
        Long comment_author_id = comment.getUser().getId();

        if (!comment_author_id.equals(author_id)) {
            throw new ForbiddenException("해당 댓글의 작성자 아님");
        }

        commentRepository.delete(comment);
        return id;
    }

    // Support 함수
    @Transactional(readOnly = true)
    public Map<Long, Long> getCommentCountsByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = commentRepository.findCommentCountsByPostIds(postIds);
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],  // postId
                        result -> (Long) result[1],  // commentCount
                        (existing, replacement) -> existing
                ));
    }

}
