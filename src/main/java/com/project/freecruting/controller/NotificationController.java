package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.notification.NotificationPageResponseDto;
import com.project.freecruting.dto.notification.UnreadCountResponseDto;
import com.project.freecruting.service.NotificationService;
import com.project.freecruting.service.infra.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    /**
     * SSE 연결 (실시간 알림 구독)
     *
     * GET /api/v1/notifications/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @LoginUser SessionUser user)
    {
        log.info("SSE 연결 요청: userId={}", user.getId());
        return sseEmitterService.subscribe(user.getId());
    }

    /**
     * 알림 목록 조회 (커서 기반 무한 스크롤)
     *
     * GET /api/v1/notifications?cursor=123&size=20
     */
    @GetMapping
    public ResponseEntity<NotificationPageResponseDto> getNotifications(

            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @LoginUser SessionUser user)
    {
        log.info("알림 목록 조회: userId={}, cursor={}, size={}, isRead={}",
                user.getId(), cursor, size, isRead);

        NotificationPageResponseDto response = notificationService.getNotifications(
                user.getId(), cursor, size, isRead);

        return ResponseEntity.ok(response);
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDto> getUnreadCount(
            @LoginUser SessionUser user
    ) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(UnreadCountResponseDto.of(count));
    }

    /**
     * 특정 알림 읽음 처리
     *
     * PATCH /api/v1/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @LoginUser SessionUser user
    ) {
        log.info("알림 읽음 처리: notificationId={}, userId={}", id, user.getId());
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     *
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            // TODO: @AuthenticationPrincipal User user
            @LoginUser SessionUser user
    ) {
        log.info("전체 알림 읽음 처리: userId={}", user.getId());
        int count = notificationService.markAllAsRead(user.getId());
        log.info("처리된 알림 개수: {}", count);
        return ResponseEntity.ok().build();
    }



}
