package com.project.freecruting.dto.comment;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class CommentSaveRequestDto {

    private String content;
    private Long post_id; // post_id, user_id 받은 후에 to Entity 호출 할 때 미리 찾아서 줌

    @Setter
    private String author;

    @Setter
    private Long user_id;

    @Builder
    public CommentSaveRequestDto(String content, Long post_id, Long user_id, String author) {
        this.content = content;
        this.post_id = post_id;
        this.user_id = user_id;
        this.author  = author;
    }

    public Comment toEntity(Post post, Users user) {
        return Comment.builder()
                .content(content)
                .post(post)
                .user(user)
                .author(author)
                .build();
    }
}
