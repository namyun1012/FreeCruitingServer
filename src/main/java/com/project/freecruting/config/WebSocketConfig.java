package com.project.freecruting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// WebSocket Server 활성화 코드
@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지를 구독하는 요청의 prefix를 /sub로 설정합니다. (예: /sub/chat/room/{roomId})
        // 클라이언트가 이 경로로 메시지를 구독하면 해당 목적지로 메시지가 브로드캐스트됩니다.
        config.enableSimpleBroker("/sub");

        // 메시지를 발행하는 요청의 prefix를 /pub으로 설정합니다. (예: /pub/chat/message)
        // 클라이언트가 이 경로로 메시지를 보내면 @MessageMapping이 붙은 컨트롤러 메서드로 라우팅됩니다.
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket Handshake를 위한 엔드포인트를 설정합니다.
        // 클라이언트는 '/ws-stomp'로 연결하여 WebSocket 통신을 시작합니다.
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // 모든 Origin 허용
                .withSockJS(); // SockJS를 사용하여 WebSocket을 지원하지 않는 브라우저에서도 동작하도록 합니다.
    }
}
