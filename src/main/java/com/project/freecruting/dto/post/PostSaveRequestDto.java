package com.project.freecruting.dto.post;

import com.project.freecruting.model.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class PostSaveRequestDto {
    private String title;
    private String content;
    private String author;
    private String imageURL;
    private String type;

    @Setter
    private String author_id;

    @Builder
    public PostSaveRequestDto(String title, String content, String author, String  imageURL, String type, String author_id) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.author = author;
        this.imageURL =imageURL;
        this.author_id = author_id;
    }

    public Post toEntity() {
        return Post.builder()
                .title(title)
                .content(content)
                .author(author)
                .imageURL(imageURL)
                .type(Post.PostType.fromString(type))
                .author_id(author_id)
                .build();
    }

}
