package com.project.freecruting.dto.user;

import com.project.freecruting.model.Post;
import com.project.freecruting.model.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSaveRequestDto {
    private String email;
    private String password;
    private String userName;

    @Builder
    public UserSaveRequestDto(String email, String password, String userName) {
        this.email = email;
        this.password = password;
        this.userName = userName;
    }

    public Users toEntity() {
        return Users.builder()
                .email(email)
                .password(password)
                .name(userName)
                .build();
    }
}
