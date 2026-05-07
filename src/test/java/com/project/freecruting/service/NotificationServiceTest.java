package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentCreatedEvent;
import com.project.freecruting.dto.notification.NotificationPageResponseDto;
import com.project.freecruting.model.Notification;
import com.project.freecruting.model.type.NotificationType;
import com.project.freecruting.model.type.ReferenceType;
import com.project.freecruting.repository.NotificationRepository;
import com.project.freecruting.service.infra.SseEmitterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SseEmitterService sseEmitterService;

    private Notification createNotification(Long id, Long userId, NotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .content("테스트 알림")
                .referenceId(1L)
                .referenceType(ReferenceType.COMMENT)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    private CommentCreatedEvent buildEvent(Long commentId, Long authorId, Long postAuthorId,
                                           Long parentCommentId, Long parentAuthorId, String content) {
        return CommentCreatedEvent.builder()
                .commentId(commentId)
                .postId(1L)
                .authorId(authorId)
                .postAuthorId(postAuthorId)
                .parentCommentId(parentCommentId)
                .parentAuthorId(parentAuthorId)
                .content(content)
                .build();
    }

    // ──────────────────────────────────────────
    // createFromComment()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("createFromComment()")
    class CreateFromComment {

        @Test
        @DisplayName("일반 댓글이면 게시글 작성자에게 POST_COMMENT 알림을 생성한다")
        void createFromComment_normalComment_notifiesPostAuthor() {
            // 댓글 작성자(authorId=10)와 게시글 작성자(postAuthorId=20)가 다름
            CommentCreatedEvent event = buildEvent(1L, 10L, 20L, null, null, "댓글 내용");
            Notification saved = createNotification(1L, 20L, NotificationType.POST_COMMENT);
            given(notificationRepository.save(any(Notification.class))).willReturn(saved);

            notificationService.createFromComment(event);

            verify(notificationRepository).save(argThat(n ->
                    n.getUserId().equals(20L) && n.getType() == NotificationType.POST_COMMENT));
        }

        @Test
        @DisplayName("답글이면 부모 댓글 작성자에게 COMMENT_REPLY 알림을 생성한다")
        void createFromComment_reply_notifiesParentAuthor() {
            // 답글 작성자(authorId=10), 부모 댓글 작성자(parentAuthorId=30)
            CommentCreatedEvent event = buildEvent(2L, 10L, 20L, 5L, 30L, "답글 내용");
            Notification saved = createNotification(2L, 30L, NotificationType.COMMENT_REPLY);
            given(notificationRepository.save(any(Notification.class))).willReturn(saved);

            notificationService.createFromComment(event);

            verify(notificationRepository).save(argThat(n ->
                    n.getUserId().equals(30L) && n.getType() == NotificationType.COMMENT_REPLY));
        }

        @Test
        @DisplayName("본인 게시글에 본인이 댓글을 달면 알림을 생성하지 않는다")
        void createFromComment_selfComment_skipsNotification() {
            // 댓글 작성자 == 게시글 작성자 (authorId == postAuthorId)
            CommentCreatedEvent event = buildEvent(3L, 10L, 10L, null, null, "본인 댓글");

            notificationService.createFromComment(event);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("알림 내용이 50자를 초과하면 말줄임표를 붙여 저장한다")
        void createFromComment_longContent_truncated() {
            String longContent = "a".repeat(60);
            CommentCreatedEvent event = buildEvent(4L, 10L, 20L, null, null, longContent);
            Notification saved = createNotification(4L, 20L, NotificationType.POST_COMMENT);
            given(notificationRepository.save(any(Notification.class))).willReturn(saved);

            notificationService.createFromComment(event);

            verify(notificationRepository).save(argThat(n ->
                    n.getContent().endsWith("...")));
        }
    }

    // ──────────────────────────────────────────
    // getNotifications() — 커서 기반 페이징 분기
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("getNotifications()")
    class GetNotifications {

        @Test
        @DisplayName("cursor=null, isRead=null이면 전체 첫 페이지를 조회한다")
        void getNotifications_noCursorNoFilter_returnsFirstPage() {
            Notification n = createNotification(1L, 1L, NotificationType.POST_COMMENT);
            given(notificationRepository.findByUserIdOrderByIdDesc(eq(1L), any()))
                    .willReturn(List.of(n));

            NotificationPageResponseDto result =
                    notificationService.getNotifications(1L, null, 10, null);

            assertThat(result.getNotifications()).hasSize(1);
            assertThat(result.getHasNext()).isFalse();
        }

        @Test
        @DisplayName("cursor=null, isRead=false이면 읽지 않은 알림 첫 페이지를 조회한다")
        void getNotifications_noCursorWithFilter_returnsFilteredFirstPage() {
            Notification n = createNotification(1L, 1L, NotificationType.POST_COMMENT);
            given(notificationRepository.findByUserIdAndIsReadOrderByIdDesc(eq(1L), eq(false), any()))
                    .willReturn(List.of(n));

            NotificationPageResponseDto result =
                    notificationService.getNotifications(1L, null, 10, false);

            assertThat(result.getNotifications()).hasSize(1);
            verify(notificationRepository).findByUserIdAndIsReadOrderByIdDesc(eq(1L), eq(false), any());
        }

        @Test
        @DisplayName("cursor가 있고 isRead=null이면 cursor 이후 전체 알림을 조회한다")
        void getNotifications_withCursorNoFilter_returnsNextPage() {
            Notification n = createNotification(1L, 1L, NotificationType.POST_COMMENT);
            given(notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(eq(1L), eq(5L), any()))
                    .willReturn(List.of(n));

            NotificationPageResponseDto result =
                    notificationService.getNotifications(1L, 5L, 10, null);

            verify(notificationRepository).findByUserIdAndIdLessThanOrderByIdDesc(eq(1L), eq(5L), any());
            assertThat(result.getHasNext()).isFalse();
        }

        @Test
        @DisplayName("결과가 size+1이면 hasNext=true이고 nextCursor를 설정한다")
        void getNotifications_hasMoreItems_setsHasNextAndCursor() {
            // size=2 요청인데 3개 반환 → hasNext=true
            Notification n1 = createNotification(3L, 1L, NotificationType.POST_COMMENT);
            Notification n2 = createNotification(2L, 1L, NotificationType.POST_COMMENT);
            Notification n3 = createNotification(1L, 1L, NotificationType.POST_COMMENT);
            given(notificationRepository.findByUserIdOrderByIdDesc(eq(1L), any()))
                    .willReturn(List.of(n1, n2, n3));

            NotificationPageResponseDto result =
                    notificationService.getNotifications(1L, null, 2, null);

            assertThat(result.getHasNext()).isTrue();
            assertThat(result.getNotifications()).hasSize(2);
            assertThat(result.getNextCursor()).isEqualTo(2L); // 마지막 항목의 ID
        }
    }

    // ──────────────────────────────────────────
    // getUnreadCount()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 개수를 반환한다")
        void getUnreadCount_returnsCount() {
            given(notificationRepository.countByUserIdAndIsReadFalse(1L)).willReturn(5L);

            long result = notificationService.getUnreadCount(1L);

            assertThat(result).isEqualTo(5L);
        }
    }

    // ──────────────────────────────────────────
    // markAsRead()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("본인 알림을 읽음 처리하면 isRead가 true가 된다")
        void markAsRead_success() {
            Notification notification = createNotification(1L, 10L, NotificationType.POST_COMMENT);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            notificationService.markAsRead(1L, 10L);

            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 알림이면 IllegalArgumentException 발생")
        void markAsRead_notFound_throwsIllegalArgumentException() {
            given(notificationRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("본인 알림이 아니면 IllegalArgumentException 발생")
        void markAsRead_wrongUser_throwsIllegalArgumentException() {
            Notification notification = createNotification(1L, 10L, NotificationType.POST_COMMENT);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ──────────────────────────────────────────
    // markAllAsRead()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("전체 읽음 처리 후 처리된 건수를 반환한다")
        void markAllAsRead_returnsCount() {
            given(notificationRepository.markAllAsReadByUserId(1L)).willReturn(3);

            int result = notificationService.markAllAsRead(1L);

            assertThat(result).isEqualTo(3);
            verify(notificationRepository).markAllAsReadByUserId(1L);
        }
    }
}
