package com.project.freecruting.controller;

import com.project.freecruting.model.ChatMessage;
import com.project.freecruting.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatController {
    private final ChatService chatService;

    // 클라이언트가 "/pub/chat/message"로 메시지를 보내면 이 메서드가 처리
    @MessageMapping("/chat/message")
    public void processChatMessage(ChatMessage chatMessage) {

        // 클라이언트에서 받은 ChatMessage 객체를 Service 계층으로 전달하여 비즈니스 로직 처리
        // Service가 메시지 저장 및 WebSocket 브로드캐스트를 모두 수행합니다.
        chatService.saveAndSendMessage(chatMessage);
    }






}
