package com.project.freecruting.service;

import com.project.freecruting.model.ChatMessage;
import com.project.freecruting.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    @Transactional
    public ChatMessage saveAndSendMessage(ChatMessage chatMessage) {
        if (ChatMessage.MessageType.JOIN.equals(chatMessage.getType())) {
            chatMessage.setMessage(chatMessage.getSender() + "님이 입장하셨습니다.");
        }

        else if(ChatMessage.MessageType.LEAVE.equals(chatMessage.getType())) {
            chatMessage.setMessage(chatMessage.getSender() + "님이 퇴장하셨습니다.");
        }

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        
        // WebSocketConfig 에서 확인
        messagingTemplate.convertAndSend("/sub/chat/room/" + savedMessage.getRoomId(), savedMessage);

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessagesByRoomId(Long roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }


}
