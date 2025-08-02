package com.project.freecruting.repository;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
    Page<Comment> findAllByPostIdOrderByModifiedDateDesc(Long postId, Pageable pageable);

    // 댓글 개수 조회 메서드
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> findCommentCountsByPostIds(@Param("postIds") List<Long> postIds);
}
