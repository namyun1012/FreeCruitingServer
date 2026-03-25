package com.project.freecruting.dto.notification;

import com.project.freecruting.model.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Getter
public class NotificationResponseDto {

    private Long id;
    private String type;
    private String content;
    private Long referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime createdDate;

    @Builder
    public NotificationResponseDto(Long id, String type, String content,
                                Long referenceId, String referenceType,
                                Boolean isRead, LocalDateTime createdDate) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.isRead = isRead;
        this.createdDate = createdDate;
    }

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .content(notification.getContent())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType().name())
                .isRead(notification.isRead())
                .build();
    }
}
