package com.project.freecruting.dto.comment;

import lombok.Builder;
import lombok.Getter;


@Getter
public class CommentCreatedEvent {

    private Long commentId;
    private Long postId;
    private Long authorId;        // 댓글 작성자
    private Long parentCommentId; // 부모 댓글 ID (nullable)
    private String content;

    // 추가 정보 (선택사항)
    private Long postAuthorId;    // 게시글 작성자
    private Long parentAuthorId;  // 부모 댓글 작성자 (nullable)

    @Builder
    public CommentCreatedEvent(Long commentId, Long postId, Long authorId,
                               Long parentCommentId, String content,
                               Long postAuthorId, Long parentAuthorId) {
        this.commentId = commentId;
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.postAuthorId = postAuthorId;
        this.parentAuthorId = parentAuthorId;
    }

    /**
     * Comment 엔티티로부터 Event 생성
     */
    public static CommentCreatedEvent from(
            Long commentId,
            Long postId,
            Long authorId,
            Long parentCommentId,
            String content,
            Long postAuthorId,
            Long parentAuthorId
    ) {
        return CommentCreatedEvent.builder()
                .commentId(commentId)
                .postId(postId)
                .authorId(authorId)
                .parentCommentId(parentCommentId)
                .content(content)
                .postAuthorId(postAuthorId)
                .parentAuthorId(parentAuthorId)
                .build();
    }

    /**
     * 답글인지 확인
     */
    public boolean isReply() {
        return parentCommentId != null;
    }
}
