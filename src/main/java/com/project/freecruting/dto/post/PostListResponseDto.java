package com.project.freecruting.dto.post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.freecruting.model.Post;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
public class PostListResponseDto {
    private Long id;
    private String title;
    private String author;
    private String imageURL;
    private String type;
    private String content;
    private LocalDateTime modifiedDate;
    private int views;
    private int comments;

    public PostListResponseDto(Post entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.author =entity.getAuthor();
        this.imageURL = entity.getImageURL();
        this.type = entity.getType().toString();
        this.modifiedDate = entity.getModifiedDate();
        this.content = entity.getContent();
        this.views = entity.getViews();
        this.comments = entity.getComments().size();
    }


    public PostListResponseDto(Post entity, Long commentCount) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.author = entity.getAuthor();
        this.imageURL = entity.getImageURL();
        this.type = entity.getType().toString();
        this.modifiedDate = entity.getModifiedDate();
        this.content = entity.getContent();
        this.views = entity.getViews();
        this.comments = commentCount != null ? commentCount.intValue() : 0;
    }

}
