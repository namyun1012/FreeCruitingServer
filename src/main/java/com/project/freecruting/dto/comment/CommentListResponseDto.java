package com.project.freecruting.dto.comment;

import com.project.freecruting.model.Comment;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentListResponseDto {
    private Long id;
    private String content;
    private Long post_id; // 추후에 수정 필요할 지도
    private Long user_id;
    private String author;
    public CommentListResponseDto(Comment entity) {
        this.id = entity.getId();
        this.content = entity.getContent();
        this.author = entity.getAuthor();
        this.post_id = entity.getPost().getId();
        this.user_id = entity.getUser().getId();
    }
}
