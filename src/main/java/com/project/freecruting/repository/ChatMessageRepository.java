package com.project.freecruting.repository;

import com.project.freecruting.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 room 에 해당하는 채팅 메시지 가져옴 (오래된 순으로)
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(Long roomId);




}
