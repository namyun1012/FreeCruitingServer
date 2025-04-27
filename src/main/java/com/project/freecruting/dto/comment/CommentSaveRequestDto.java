package com.project.freecruting.dto.comment;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentSaveRequestDto {

    private String content;
    private Long post_id; // post_id, user_id 받은 후에 to Entity 호출 할 때 미리 찾아서 줌

    @Builder
    public CommentSaveRequestDto(String content, Long post_id) {
        this.content = content;
        this.post_id = post_id;
    }

    public Comment toEntity(Post post, User user) {
        return Comment.builder()
                .content(content)
                .post(post)
                .user(user)
                .author(user.getName())
                .build();
    }
}
