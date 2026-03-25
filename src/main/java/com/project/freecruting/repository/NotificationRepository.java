package com.project.freecruting.repository;

import com.project.freecruting.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 커서 기반 페이징 - 첫 페이지 조회
     */
    List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);


    /**
     * 커서 기반 페이징 - 다음 페이지 조회
     */
    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(
            Long userId,
            Long cursor,
            Pageable pageable
    );

    /**
     * 읽지 않은 알림 개수 조회
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 모든 알림 읽음 처리 (Bulk Update)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 오래된 읽은 알림 삭제 (선택사항)
     * 예: 30일 이상 지난 읽은 알림 정리
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.isRead = true AND n.createdDate < :beforeDate")
    int deleteOldReadNotifications(@Param("userId") Long userId, @Param("beforeDate") java.time.LocalDateTime beforeDate);
}
