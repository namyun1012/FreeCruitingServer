package com.project.freecruting.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequestDto {
    private String name;
    private String picture;

    @Builder
    public UserUpdateRequestDto(String name, String picture) {
        this.name = name;
        this.picture = picture;
    }
}