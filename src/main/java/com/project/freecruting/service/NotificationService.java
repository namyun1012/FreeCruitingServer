package com.project.freecruting.service;

import com.project.freecruting.dto.notification.NotificationPageResponseDto;
import com.project.freecruting.dto.comment.CommentCreatedEvent;
import com.project.freecruting.model.Notification;
import com.project.freecruting.model.type.NotificationType;
import com.project.freecruting.model.type.ReferenceType;
import com.project.freecruting.repository.NotificationRepository;
import com.project.freecruting.service.infra.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    /**
     * 댓글 생성 이벤트로부터 알림 생성
     */
    @Transactional
    public void createFromComment(CommentCreatedEvent event) {
        log.info("=== createFromComment 시작 ===");
        log.info("Event: commentId={}, postAuthorId={}, authorId={}",
                event.getCommentId(), event.getPostAuthorId(), event.getAuthorId());

        // 알림 수신 대상자 결정
        Long recipientId = determineRecipient(event);
        log.info("결정된 수신자: recipientId={}", recipientId);
        // 자기 자신에게는 알림 보내지 않음

        if (recipientId == null || recipientId.equals(event.getAuthorId())) {
            log.debug("알림 수신 대상 없음 또는 본인: commentId={}", event.getCommentId());
            return;
        }

        log.info("알림 생성 진행...");
        // 알림 타입 결정
        NotificationType type = event.isReply()
                ? NotificationType.COMMENT_REPLY
                : NotificationType.POST_COMMENT;

        String content = generateContent(event, type);

        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(type)
                .content(content)
                .referenceId(event.getCommentId())
                .referenceType(ReferenceType.COMMENT)
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("알림 생성: id={}, userId={}, type={}",
                saved.getId(), recipientId, type);

        // SSE로 실시간 전송
        sendRealtime(saved);
    }

    /**
     * SSE 실시간 전송
     */
    public void sendRealtime(Notification notification) {
        try {
            sseEmitterService.send(notification.getUserId(), notification);
        } catch (Exception e) {
            log.error("실시간 알림 전송 실패: notificationId={}",
                    notification.getId(), e);
            // 전송 실패해도 DB에는 저장되어 있으므로 나중에 조회 가능
        }
    }

    /**
     * 커서 기반 알림 목록 조회
     */
    public NotificationPageResponseDto getNotifications(Long userId, Long cursor, int size) { {
        List<Notification> notifications;

        if (cursor == null) {
            // 첫 페이지
            notifications = notificationRepository.findByUserIdOrderByIdDesc(
                    userId, PageRequest.of(0, size + 1));
        } else {
            // 다음 페이지
            notifications = notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(
                    userId, cursor, PageRequest.of(0, size + 1));
        }

        // hasNext 판단
        boolean hasNext = notifications.size() > size;
        if (hasNext) {
            notifications = notifications.subList(0, size);
        }

        // nextCursor 계산
        Long nextCursor = hasNext && !notifications.isEmpty()
                ? notifications.get(notifications.size() - 1).getId()
                : null;

        return NotificationPageResponseDto.of(notifications, nextCursor, hasNext);
    }}

    /**
     * 읽지 않은 알림 개수 조회
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

        // 본인의 알림인지 확인
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 알림만 처리할 수 있습니다");
        }

        notification.markAsRead();
        log.info("알림 읽음 처리: id={}", notificationId);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("전체 알림 읽음 처리: userId={}, count={}", userId, count);
        return count;
    }



    /**
     * 알림 수신 대상자 결정
     */
    private Long determineRecipient(CommentCreatedEvent event) {
        // 답글인 경우: 부모 댓글 작성자
        if (event.isReply()) {
            return event.getParentAuthorId();
        }
        // 일반 댓글인 경우: 게시글 작성자
        return event.getPostAuthorId();
    }





    /**
     * 알림 내용 생성
     */
    private String generateContent(CommentCreatedEvent event, NotificationType type) {
        String preview = event.getContent().length() > 50
                ? event.getContent().substring(0, 50) + "..."
                : event.getContent();

        return switch (type) {
            case COMMENT_REPLY -> "회원님의 댓글에 답글이 달렸습니다: " + preview;
            case POST_COMMENT -> "회원님의 게시글에 댓글이 달렸습니다: " + preview;
            case DEFAULT_NOTIFICATION -> preview;
            default -> preview;
        };
    }

}
