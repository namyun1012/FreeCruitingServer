package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 우선 파티 당 채팅방 1개씩을 목표로 하나
// ChatMessage 가 파티에 완전히 종속되는 것은 피함


@Getter
@NoArgsConstructor
@Entity
@AllArgsConstructor
public class ChatMessage {

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private Long partyId;

    @Column(nullable = false)
    private Long roomId;


    private String sender;

    @Setter
    @Column(length = 1000)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if(this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
