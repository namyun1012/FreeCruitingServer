package com.project.freecruting.service.infra;

import com.project.freecruting.dto.notification.NotificationDto;
import com.project.freecruting.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    // 사용자 ID별 SseEmitter 관리
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 타임아웃 (1시간)
    private static final Long TIMEOUT = 60 * 60 * 1000L;

    /**
     * SSE 연결 생성
     */
    public SseEmitter subscribe(Long userId) {
        String emitterId = getEmitterId(userId);
        SseEmitter emitter = new SseEmitter(TIMEOUT);


        // 연결 완료/타임아웃/에러 시 emitter 제거
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(emitterId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            emitters.remove(emitterId);
        });

        emitter.onError((e) -> {
            log.error("SSE 연결 에러: userId={}", userId, e);
            emitters.remove(emitterId);
        });

        // emitter 저장
        emitters.put(emitterId, emitter);

        // 연결 직후 더미 이벤트 전송 (503 에러 방지)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected to notification stream"));
            log.info("SSE 연결 성공: userId={}", userId);
        } catch (IOException e) {
            log.error("SSE 초기 연결 실패: userId={}", userId, e);
            emitters.remove(emitterId);
            throw new RuntimeException("SSE 연결 실패");
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void send(Long userId, Notification notification) {
        String emitterId = getEmitterId(userId);
        SseEmitter emitter = emitters.get(emitterId);

        if (emitter == null) {
            log.debug("SSE 연결 없음: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(NotificationDto.from(notification)));
            log.info("SSE 알림 전송 성공: userId={}, notificationId={}",
                    userId, notification.getId());
        } catch (IOException e) {
            log.error("SSE 알림 전송 실패: userId={}, notificationId={}",
                    userId, notification.getId(), e);
            emitters.remove(emitterId);
        }
    }

    /**
     * Heartbeat 전송 (연결 유지용)
     * 스케줄러로 주기적 호출 가능
     */
    public void sendHeartbeat(Long userId) {
        String emitterId = getEmitterId(userId);
        SseEmitter emitter = emitters.get(emitterId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping"));
        } catch (IOException e) {
            log.error("Heartbeat 전송 실패: userId={}", userId, e);
            emitters.remove(emitterId);
        }

    }

    /**
     * 현재 연결된 사용자 수
     */
    public int getConnectionCount() {
        return emitters.size();
    }

    private String getEmitterId(Long userId) {
        return "user_" + userId;
    }


}
