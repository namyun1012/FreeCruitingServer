package com.project.freecruting.dto.notification;


import lombok.Builder;
import lombok.Getter;

@Getter
public class UnreadCountResponseDto {

    private Long count;

    @Builder
    public UnreadCountResponseDto(Long count) {
        this.count = count;
    }

    public static UnreadCountResponseDto of(long count) {
        return UnreadCountResponseDto.builder()
                .count(count)
                .build();
    }


}
