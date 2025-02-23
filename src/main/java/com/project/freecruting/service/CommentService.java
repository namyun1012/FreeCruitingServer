package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentListResponseDto;
import com.project.freecruting.dto.comment.CommentSaveRequestDto;
import com.project.freecruting.dto.comment.CommentUpdateRequestDto;
import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.Users;
import com.project.freecruting.repository.CommentRepository;
import com.project.freecruting.repository.PostRepository;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional
    public Long save(CommentSaveRequestDto requestDto) {
        Long post_id = requestDto.getPost_id();
        Long user_id = requestDto.getUser_id();
        Post post = postRepository.findById(post_id).orElseThrow(() -> new RuntimeException("해당 POST 없음"));
        Users user = userRepository.findById(user_id).orElseThrow(() -> new RuntimeException("해당 USER 없음"));

        return commentRepository.save(requestDto.toEntity(post,user)).getId();
    }

    @Transactional
    public Long update(Long id, CommentUpdateRequestDto requestDto, Long author_id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 COMMENT 없음. id=" + id));
        Long comment_author_id = comment.getUser().getId();

        if (!comment_author_id.equals(author_id)) {
            return 0L;
        }

        comment.update(requestDto.getContent());
        return id;
    }

    @Transactional
    public Long delete(Long id, Long author_id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 COMMENT 없음. id = " + id));
        Long comment_author_id = comment.getUser().getId();

        if (!comment_author_id.equals(author_id)) {
            return 0L;
        }

        commentRepository.delete(comment);
        return id;
    }
}
