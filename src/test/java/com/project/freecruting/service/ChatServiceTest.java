package com.project.freecruting.service;

import com.project.freecruting.model.ChatMessage;
import com.project.freecruting.repository.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private ChatMessage createChatMessage(Long id, ChatMessage.MessageType type,
                                          Long roomId, String sender, String message) {
        ChatMessage msg = new ChatMessage(null, type, 1L, roomId, sender, message, LocalDateTime.now());
        ReflectionTestUtils.setField(msg, "id", id);
        return msg;
    }

    // ──────────────────────────────────────────
    // saveAndSendMessage()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("saveAndSendMessage()")
    class SaveAndSendMessage {

        @Test
        @DisplayName("CHAT 타입 메시지는 message를 그대로 저장하고 STOMP로 전송한다")
        void saveAndSendMessage_chatType_savesAndSends() {
            ChatMessage input = createChatMessage(null, ChatMessage.MessageType.CHAT,
                    100L, "홍길동", "안녕하세요");
            ChatMessage saved = createChatMessage(1L, ChatMessage.MessageType.CHAT,
                    100L, "홍길동", "안녕하세요");
            given(chatMessageRepository.save(input)).willReturn(saved);

            ChatMessage result = chatService.saveAndSendMessage(input);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getMessage()).isEqualTo("안녕하세요");
            verify(messagingTemplate).convertAndSend(eq("/sub/chat/room/100"), eq(saved));
        }

        @Test
        @DisplayName("JOIN 타입 메시지는 '○○님이 입장하셨습니다'로 message를 덮어쓴다")
        void saveAndSendMessage_joinType_overridesMessage() {
            ChatMessage input = createChatMessage(null, ChatMessage.MessageType.JOIN,
                    100L, "홍길동", null);
            ChatMessage saved = createChatMessage(2L, ChatMessage.MessageType.JOIN,
                    100L, "홍길동", "홍길동님이 입장하셨습니다.");
            given(chatMessageRepository.save(input)).willReturn(saved);

            chatService.saveAndSendMessage(input);

            assertThat(input.getMessage()).isEqualTo("홍길동님이 입장하셨습니다.");
            verify(chatMessageRepository).save(input);
        }

        @Test
        @DisplayName("LEAVE 타입 메시지는 '○○님이 퇴장하셨습니다'로 message를 덮어쓴다")
        void saveAndSendMessage_leaveType_overridesMessage() {
            ChatMessage input = createChatMessage(null, ChatMessage.MessageType.LEAVE,
                    100L, "홍길동", null);
            ChatMessage saved = createChatMessage(3L, ChatMessage.MessageType.LEAVE,
                    100L, "홍길동", "홍길동님이 퇴장하셨습니다.");
            given(chatMessageRepository.save(input)).willReturn(saved);

            chatService.saveAndSendMessage(input);

            assertThat(input.getMessage()).isEqualTo("홍길동님이 퇴장하셨습니다.");
            verify(chatMessageRepository).save(input);
        }

        @Test
        @DisplayName("저장 후 올바른 topic(/sub/chat/room/{roomId})으로 STOMP 메시지를 전송한다")
        void saveAndSendMessage_sendsToCorrectTopic() {
            Long roomId = 200L;
            ChatMessage input = createChatMessage(null, ChatMessage.MessageType.CHAT,
                    roomId, "테스터", "테스트 메시지");
            ChatMessage saved = createChatMessage(10L, ChatMessage.MessageType.CHAT,
                    roomId, "테스터", "테스트 메시지");
            given(chatMessageRepository.save(input)).willReturn(saved);

            chatService.saveAndSendMessage(input);

            verify(messagingTemplate).convertAndSend(
                    eq("/sub/chat/room/" + roomId), eq(saved));
        }
    }

    // ──────────────────────────────────────────
    // getChatMessagesByRoomId()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("getChatMessagesByRoomId()")
    class GetChatMessagesByRoomId {

        @Test
        @DisplayName("roomId로 채팅 메시지 목록을 시간순으로 반환한다")
        void getChatMessagesByRoomId_returnsList() {
            Long roomId = 100L;
            ChatMessage m1 = createChatMessage(1L, ChatMessage.MessageType.CHAT, roomId, "A", "첫 메시지");
            ChatMessage m2 = createChatMessage(2L, ChatMessage.MessageType.CHAT, roomId, "B", "두번째");
            given(chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId))
                    .willReturn(List.of(m1, m2));

            List<ChatMessage> result = chatService.getChatMessagesByRoomId(roomId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMessage()).isEqualTo("첫 메시지");
        }

        @Test
        @DisplayName("채팅 메시지가 없으면 빈 리스트를 반환한다")
        void getChatMessagesByRoomId_noMessages_returnsEmptyList() {
            given(chatMessageRepository.findByRoomIdOrderByTimestampAsc(anyLong()))
                    .willReturn(Collections.emptyList());

            List<ChatMessage> result = chatService.getChatMessagesByRoomId(999L);

            assertThat(result).isEmpty();
        }
    }
}
