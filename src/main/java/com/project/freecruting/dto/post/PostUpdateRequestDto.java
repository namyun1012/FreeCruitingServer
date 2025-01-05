package com.project.freecruting.dto.post;

import com.project.freecruting.model.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostUpdateRequestDto {
    private String title;
    private String content;
    private String imageURL;
    private String type;

    @Builder
    public PostUpdateRequestDto(String title, String content, String  imageURL, String type) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.imageURL =imageURL;
    }
}
