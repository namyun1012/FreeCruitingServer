package com.project.freecruting.dto.notification;


import com.project.freecruting.model.Notification;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class NotificationPageResponseDto {

    private List<NotificationResponseDto> notifications;
    private Long nextCursor;
    private Boolean hasNext;

    @Builder
    public NotificationPageResponseDto(List<NotificationResponseDto> notifications,
                                    Long nextCursor, Boolean hasNext) {
        this.notifications = notifications;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static NotificationPageResponseDto of(
            List<Notification> notifications,
            Long nextCursor,
            boolean hasNext
    ) {
        List<NotificationResponseDto> responses = notifications.stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());

        return NotificationPageResponseDto.builder()
                .notifications(responses)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }



}
