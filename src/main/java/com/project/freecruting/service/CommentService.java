package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentCreatedEvent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; // ⭐ Event 발행용 추가

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
        Post post = postRepository.findById(post_id)
                .orElseThrow(() -> new NotFoundException("해당 POST 없음"));
        User user = userRepository.findById(user_id)
                .orElseThrow(() -> new NotFoundException("해당 USER 없음"));

        // 댓글 저장
        Comment savedComment = commentRepository.save(requestDto.toEntity(post, user));

        log.info("댓글 생성 완료: commentId={}, postId={}, userId={}",
                savedComment.getId(), post_id, user_id);

        // ⭐ Event 발행
        publishCommentCreatedEvent(savedComment, post, user);

        return savedComment.getId();
    }

    @Transactional
    public Long update(Long id, CommentUpdateRequestDto requestDto, Long author_id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 COMMENT 없음. id=" + id));
        Long comment_author_id = comment.getUser().getId();

        if (!comment_author_id.equals(author_id)) {
            throw new ForbiddenException("해당 댓글의 작성자 아님");
        }

        comment.update(requestDto.getContent());
        return id;
    }

    @Transactional
    public Long delete(Long id, Long author_id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 COMMENT 없음. id = " + id));
        Long comment_author_id = comment.getUser().getId();

        if (!comment_author_id.equals(author_id)) {
            throw new ForbiddenException("해당 댓글의 작성자 아님");
        }

        commentRepository.delete(comment);
        return id;
    }

    // ⭐ Event 발행 메서드 추가
    private void publishCommentCreatedEvent(Comment comment, Post post, User author) {
        try {
            CommentCreatedEvent event = CommentCreatedEvent.builder()
                    .commentId(comment.getId())
                    .postId(post.getId())
                    .authorId(author.getId())
                    .parentCommentId(null) // 답글 미구현 상태이므로 null
                    .content(comment.getContent())
                    .postAuthorId(post.getAuthor_id()) // 게시글 작성자 ID
                    .parentAuthorId(null) // 답글 미구현 상태이므로 null
                    .build();

            eventPublisher.publishEvent(event);
            log.info("CommentCreatedEvent 발행 성공: commentId={}", comment.getId());
        } catch (Exception e) {
            log.error("CommentCreatedEvent 발행 실패: commentId={}", comment.getId(), e);
            // Event 발행 실패해도 댓글 생성은 성공으로 처리
        }
    }
}