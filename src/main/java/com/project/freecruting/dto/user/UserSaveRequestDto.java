package com.project.freecruting.dto.user;

import com.project.freecruting.model.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSaveRequestDto {

    @NotNull(message = "이메일 필수")
    private String email;
    
    @NotNull(message = "비밀번호 필수")
    @Size(min = 8, message = "비밀 번호 8자 이상")
    private String password;
    private String name;

    @Builder
    public UserSaveRequestDto(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public User toEntity() {
        return User.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
    }
}
