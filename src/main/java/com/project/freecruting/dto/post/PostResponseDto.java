package com.project.freecruting.dto.post;

import com.project.freecruting.model.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private String author;
    private String imageURL;
    private String type;

    public PostResponseDto(Post entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.content = entity.getContent();
        this.author = entity.getAuthor();
        this.imageURL = entity.getImageURL();
        this.type   = entity.getType();
    }

}
