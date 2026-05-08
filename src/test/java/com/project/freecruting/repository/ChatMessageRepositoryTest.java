package com.project.freecruting.repository;

import com.project.freecruting.model.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private ChatMessage saveMessage(Long roomId, String sender, String message, LocalDateTime timestamp) {
        return chatMessageRepository.save(new ChatMessage(
                null,
                ChatMessage.MessageType.CHAT,
                null,
                roomId,
                sender,
                message,
                timestamp
        ));
    }

    // ──────────────────────────────────────────
    // findByRoomIdOrderByTimestampAsc() — 채팅방 메시지 시간순 조회
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByRoomIdOrderByTimestampAsc()")
    class FindByRoomIdOrderByTimestampAsc {

        @Test
        @DisplayName("해당 방의 메시지를 타임스탬프 오름차순으로 반환한다")
        void findByRoomIdOrderByTimestampAsc_returnsAscendingOrder() {
            LocalDateTime base = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
            saveMessage(1L, "Alice", "첫번째", base);
            saveMessage(1L, "Bob",   "두번째", base.plusMinutes(1));
            saveMessage(1L, "Alice", "세번째", base.plusMinutes(2));

            List<ChatMessage> result = chatMessageRepository.findByRoomIdOrderByTimestampAsc(1L);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getMessage()).isEqualTo("첫번째");
            assertThat(result.get(1).getMessage()).isEqualTo("두번째");
            assertThat(result.get(2).getMessage()).isEqualTo("세번째");
        }

        @Test
        @DisplayName("타임스탬프 오름차순 정렬이 보장된다")
        void findByRoomIdOrderByTimestampAsc_timestampOrderIsCorrect() {
            LocalDateTime base = LocalDateTime.of(2024, 6, 1, 9, 0, 0);
            saveMessage(1L, "C", "나중", base.plusMinutes(10));
            saveMessage(1L, "A", "먼저", base);
            saveMessage(1L, "B", "중간", base.plusMinutes(5));

            List<ChatMessage> result = chatMessageRepository.findByRoomIdOrderByTimestampAsc(1L);

            assertThat(result.get(0).getTimestamp()).isBefore(result.get(1).getTimestamp());
            assertThat(result.get(1).getTimestamp()).isBefore(result.get(2).getTimestamp());
        }

        @Test
        @DisplayName("다른 방의 메시지는 결과에 포함되지 않는다")
        void findByRoomIdOrderByTimestampAsc_filtersOtherRooms() {
            LocalDateTime now = LocalDateTime.now();
            saveMessage(1L, "Alice", "방1 메시지", now);
            saveMessage(2L, "Bob",   "방2 메시지", now);

            List<ChatMessage> result = chatMessageRepository.findByRoomIdOrderByTimestampAsc(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoomId()).isEqualTo(1L);
            assertThat(result.get(0).getMessage()).isEqualTo("방1 메시지");
        }

        @Test
        @DisplayName("메시지가 없는 방은 빈 리스트를 반환한다")
        void findByRoomIdOrderByTimestampAsc_noMessages_returnsEmpty() {
            List<ChatMessage> result = chatMessageRepository.findByRoomIdOrderByTimestampAsc(99L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("JOIN/LEAVE 타입 메시지도 결과에 포함된다")
        void findByRoomIdOrderByTimestampAsc_includesJoinLeaveMessages() {
            LocalDateTime base = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
            chatMessageRepository.save(new ChatMessage(
                    null, ChatMessage.MessageType.JOIN, null, 1L, "Alice", null, base));
            chatMessageRepository.save(new ChatMessage(
                    null, ChatMessage.MessageType.CHAT, null, 1L, "Alice", "안녕", base.plusMinutes(1)));
            chatMessageRepository.save(new ChatMessage(
                    null, ChatMessage.MessageType.LEAVE, null, 1L, "Alice", null, base.plusMinutes(2)));

            List<ChatMessage> result = chatMessageRepository.findByRoomIdOrderByTimestampAsc(1L);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getType()).isEqualTo(ChatMessage.MessageType.JOIN);
            assertThat(result.get(1).getType()).isEqualTo(ChatMessage.MessageType.CHAT);
            assertThat(result.get(2).getType()).isEqualTo(ChatMessage.MessageType.LEAVE);
        }
    }
}
