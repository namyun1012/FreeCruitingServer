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


    @Builder
    public PostSaveRequestDto(String title, String content, String author, String  imageURL, String type) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.author = author;
        this.imageURL =imageURL;
    }

    public Post toEntity(Long author_id) {
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
