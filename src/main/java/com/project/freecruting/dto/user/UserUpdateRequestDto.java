package com.project.freecruting.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequestDto {

    @NotNull(message = "이름 필수")
    @Size(min = 1, message = "빈 값은 안 됩니다")
    private String name;


    private String picture;

    @Builder
    public UserUpdateRequestDto(String name, String picture) {
        this.name = name;
        this.picture = picture;
    }
}