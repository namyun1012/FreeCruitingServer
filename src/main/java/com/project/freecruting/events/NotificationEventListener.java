package com.project.freecruting.events;


import com.project.freecruting.dto.comment.CommentCreatedEvent;
import com.project.freecruting.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * 댓글 생성 이벤트 처리
     * @Async로 비동기 처리 (댓글 생성 성능에 영향 없음)
     */
    @Async
    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("댓글 생성 이벤트 수신: commentId={}, postId={}",
                event.getCommentId(), event.getPostId());

        try {
            notificationService.createFromComment(event);
        } catch (Exception e) {
            log.error("알림 생성 실패: commentId={}", event.getCommentId(), e);
            // 알림 생성 실패해도 댓글 생성 자체는 성공
            // 필요시 재시도 로직 추가 가능
        }
    }



}
