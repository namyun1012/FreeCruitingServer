package com.project.freecruting.repository;

import com.project.freecruting.config.JpaAuditingConfig;
import com.project.freecruting.model.Notification;
import com.project.freecruting.model.type.NotificationType;
import com.project.freecruting.model.type.ReferenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// deleteOldReadNotifications() 가 createdDate 를 기준으로 삭제하므로
// JPA Auditing 을 활성화하여 @CreatedDate 가 자동 설정되도록 한다.
@DataJpaTest
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long USER_ID       = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private Notification saveNotification(Long userId, boolean read) {
        Notification n = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.DEFAULT_NOTIFICATION)
                .content("알림 내용")
                .referenceId(1L)
                .referenceType(ReferenceType.DEFAULT)
                .build());
        if (read) {
            n.markAsRead();
            notificationRepository.save(n);
        }
        return n;
    }

    // ──────────────────────────────────────────
    // findByUserIdOrderByIdDesc() — 첫 페이지 커서 조회
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByUserIdOrderByIdDesc()")
    class FindByUserIdOrderByIdDesc {

        @Test
        @DisplayName("해당 사용자의 알림을 ID 내림차순으로 반환한다")
        void findByUserIdOrderByIdDesc_returnsDescendingOrder() {
            saveNotification(USER_ID, false);
            saveNotification(USER_ID, false);
            saveNotification(OTHER_USER_ID, false);

            List<Notification> result = notificationRepository
                    .findByUserIdOrderByIdDesc(USER_ID, PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(n -> n.getUserId().equals(USER_ID));
            assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
        }

        @Test
        @DisplayName("size 만큼만 반환한다")
        void findByUserIdOrderByIdDesc_respectsPageSize() {
            for (int i = 0; i < 5; i++) saveNotification(USER_ID, false);

            List<Notification> result = notificationRepository
                    .findByUserIdOrderByIdDesc(USER_ID, PageRequest.of(0, 3));

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("알림이 없으면 빈 리스트를 반환한다")
        void findByUserIdOrderByIdDesc_noNotifications_returnsEmpty() {
            List<Notification> result = notificationRepository
                    .findByUserIdOrderByIdDesc(USER_ID, PageRequest.of(0, 10));

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findByUserIdAndIdLessThanOrderByIdDesc() — 커서 다음 페이지
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByUserIdAndIdLessThanOrderByIdDesc()")
    class FindByUserIdAndIdLessThanOrderByIdDesc {

        @Test
        @DisplayName("커서 ID보다 작은 알림만 반환한다")
        void findByUserIdAndIdLessThan_returnsBelowCursor() {
            Notification n1 = saveNotification(USER_ID, false);
            Notification n2 = saveNotification(USER_ID, false);
            Notification n3 = saveNotification(USER_ID, false); // 가장 최신

            List<Notification> result = notificationRepository
                    .findByUserIdAndIdLessThanOrderByIdDesc(USER_ID, n3.getId(), PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(n -> n.getId() < n3.getId());
        }

        @Test
        @DisplayName("커서보다 작은 알림이 없으면 빈 리스트를 반환한다")
        void findByUserIdAndIdLessThan_noBelowCursor_returnsEmpty() {
            Notification n1 = saveNotification(USER_ID, false);

            List<Notification> result = notificationRepository
                    .findByUserIdAndIdLessThanOrderByIdDesc(USER_ID, n1.getId(), PageRequest.of(0, 10));

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // findByUserIdAndIsReadOrderByIdDesc() — 읽음 여부 필터 첫 페이지
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByUserIdAndIsReadOrderByIdDesc()")
    class FindByUserIdAndIsReadOrderByIdDesc {

        @Test
        @DisplayName("읽지 않은 알림만 반환한다")
        void findByUserIdAndIsRead_returnsUnreadOnly() {
            saveNotification(USER_ID, false);
            saveNotification(USER_ID, false);
            saveNotification(USER_ID, true);

            List<Notification> result = notificationRepository
                    .findByUserIdAndIsReadOrderByIdDesc(USER_ID, false, PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(n -> !n.isRead());
        }

        @Test
        @DisplayName("읽은 알림만 반환한다")
        void findByUserIdAndIsRead_returnsReadOnly() {
            saveNotification(USER_ID, true);
            saveNotification(USER_ID, false);

            List<Notification> result = notificationRepository
                    .findByUserIdAndIsReadOrderByIdDesc(USER_ID, true, PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isRead()).isTrue();
        }
    }

    // ──────────────────────────────────────────
    // findByUserIdAndIsReadAndIdLessThanOrderByIdDesc() — 읽음 필터 + 커서
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByUserIdAndIsReadAndIdLessThanOrderByIdDesc()")
    class FindByUserIdAndIsReadAndIdLessThan {

        @Test
        @DisplayName("읽지 않은 알림 중 커서 ID 이전 항목만 반환한다")
        void findByUserIdAndIsReadAndIdLessThan_combinedFilter() {
            Notification older1 = saveNotification(USER_ID, false);
            Notification older2 = saveNotification(USER_ID, false);
            Notification cursor = saveNotification(USER_ID, false);

            List<Notification> result = notificationRepository
                    .findByUserIdAndIsReadAndIdLessThanOrderByIdDesc(
                            USER_ID, false, cursor.getId(), PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(n -> n.getId() < cursor.getId() && !n.isRead());
        }

        @Test
        @DisplayName("읽은 알림은 읽지 않음 커서 조회에서 제외된다")
        void findByUserIdAndIsReadAndIdLessThan_excludesRead() {
            Notification unread = saveNotification(USER_ID, false);
            Notification read   = saveNotification(USER_ID, true);
            Notification cursor = saveNotification(USER_ID, false);

            List<Notification> result = notificationRepository
                    .findByUserIdAndIsReadAndIdLessThanOrderByIdDesc(
                            USER_ID, false, cursor.getId(), PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(unread.getId());
        }
    }

    // ──────────────────────────────────────────
    // countByUserIdAndIsReadFalse() — 읽지 않은 알림 수
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("countByUserIdAndIsReadFalse()")
    class CountByUserIdAndIsReadFalse {

        @Test
        @DisplayName("읽지 않은 알림 수만 집계한다")
        void countByUserIdAndIsReadFalse_countsOnlyUnread() {
            saveNotification(USER_ID, false);
            saveNotification(USER_ID, false);
            saveNotification(USER_ID, true);
            saveNotification(OTHER_USER_ID, false);

            long count = notificationRepository.countByUserIdAndIsReadFalse(USER_ID);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("읽지 않은 알림이 없으면 0을 반환한다")
        void countByUserIdAndIsReadFalse_allRead_returnsZero() {
            saveNotification(USER_ID, true);

            long count = notificationRepository.countByUserIdAndIsReadFalse(USER_ID);

            assertThat(count).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────
    // markAllAsReadByUserId() — 일괄 읽음 처리
    // flush + clear 로 1차 캐시 무효화 후 검증
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("markAllAsReadByUserId()")
    class MarkAllAsReadByUserId {

        @Test
        @DisplayName("해당 사용자의 모든 읽지 않은 알림을 읽음으로 처리하고 변경 수를 반환한다")
        void markAllAsReadByUserId_marksAllUnreadAsRead() {
            saveNotification(USER_ID, false);
            saveNotification(USER_ID, false);
            saveNotification(OTHER_USER_ID, false);
            entityManager.flush();

            int updated = notificationRepository.markAllAsReadByUserId(USER_ID);
            entityManager.clear();

            assertThat(updated).isEqualTo(2);
            assertThat(notificationRepository.countByUserIdAndIsReadFalse(USER_ID)).isEqualTo(0);
            // 다른 사용자의 알림은 변경되지 않아야 한다
            assertThat(notificationRepository.countByUserIdAndIsReadFalse(OTHER_USER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 모두 읽은 경우 영향받은 행이 0이다")
        void markAllAsReadByUserId_alreadyAllRead_returnsZero() {
            saveNotification(USER_ID, true);
            entityManager.flush();

            int updated = notificationRepository.markAllAsReadByUserId(USER_ID);

            assertThat(updated).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────
    // deleteOldReadNotifications() — 오래된 읽은 알림 삭제
    // @Import(JpaAuditingConfig.class) 로 createdDate 자동 설정
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("deleteOldReadNotifications()")
    class DeleteOldReadNotifications {

        @Test
        @DisplayName("기준 날짜 이전에 생성된 읽은 알림만 삭제한다")
        void deleteOldReadNotifications_deletesOnlyOldReadNotifications() {
            saveNotification(USER_ID, true);   // 읽음 → 삭제 대상
            saveNotification(USER_ID, true);   // 읽음 → 삭제 대상
            saveNotification(USER_ID, false);  // 읽지 않음 → 보존
            entityManager.flush();

            // 방금 생성된 알림이므로 1분 뒤를 기준으로 설정하면 모두 포함
            LocalDateTime future = LocalDateTime.now().plusMinutes(1);
            int deleted = notificationRepository.deleteOldReadNotifications(USER_ID, future);
            entityManager.clear();

            assertThat(deleted).isEqualTo(2);
            List<Notification> remaining = notificationRepository
                    .findByUserIdOrderByIdDesc(USER_ID, PageRequest.of(0, 10));
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).isRead()).isFalse();
        }

        @Test
        @DisplayName("과거 날짜를 기준으로 하면 삭제 대상이 없다")
        void deleteOldReadNotifications_pastCutoff_deletesNothing() {
            saveNotification(USER_ID, true);
            entityManager.flush();

            LocalDateTime past = LocalDateTime.now().minusDays(1);
            int deleted = notificationRepository.deleteOldReadNotifications(USER_ID, past);

            assertThat(deleted).isEqualTo(0);
        }

        @Test
        @DisplayName("다른 사용자의 읽은 알림은 삭제되지 않는다")
        void deleteOldReadNotifications_doesNotAffectOtherUsers() {
            saveNotification(OTHER_USER_ID, true);
            entityManager.flush();

            LocalDateTime future = LocalDateTime.now().plusMinutes(1);
            int deleted = notificationRepository.deleteOldReadNotifications(USER_ID, future);

            assertThat(deleted).isEqualTo(0);
            assertThat(notificationRepository
                    .findByUserIdOrderByIdDesc(OTHER_USER_ID, PageRequest.of(0, 10))).hasSize(1);
        }
    }
}
