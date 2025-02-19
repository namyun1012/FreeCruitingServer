package com.project.freecruting.dto.post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.freecruting.model.Post;
import lombok.Getter;

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

    public PostListResponseDto(Post entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.author =entity.getAuthor();
        this.imageURL = entity.getImageURL();
        this.type = entity.getType().toString();
        this.modifiedDate = entity.getModifiedDate();
        this.content = entity.getContent();
    }

}
