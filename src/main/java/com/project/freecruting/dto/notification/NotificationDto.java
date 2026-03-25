package com.project.freecruting.dto.notification;

import com.project.freecruting.model.Notification;

public class NotificationDto {
    public Long id;
    public String type;
    public String content;
    public Long referenceId;
    public String referenceType;
    public boolean isRead;
    public String createdDate;

    public static NotificationDto from(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.id = notification.getId();
        dto.type = notification.getType().name();
        dto.content = notification.getContent();
        dto.referenceId = notification.getReferenceId();
        dto.referenceType = notification.getReferenceType().name();
        dto.isRead = notification.isRead();
        dto.createdDate = notification.getCreatedDate().toString();
        return dto;
    }

}
