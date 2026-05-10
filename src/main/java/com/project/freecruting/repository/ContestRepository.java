package com.project.freecruting.repository;

import com.project.freecruting.model.Contest;
import com.project.freecruting.model.type.ContestCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    Page<Contest> findAllByOrderByIdDesc(Pageable pageable);

    Page<Contest> findByCategoryOrderByIdDesc(ContestCategory category, Pageable pageable);

    @Query(value = "SELECT * FROM contest c WHERE " +
            "c.title LIKE %:keyword% OR " +
            "c.organizer LIKE %:keyword% OR " +
            "c.description LIKE %:keyword% ORDER BY c.id DESC",
            countQuery = "SELECT count(*) FROM contest c WHERE " +
                    "c.title LIKE %:keyword% OR " +
                    "c.organizer LIKE %:keyword% OR " +
                    "c.description LIKE %:keyword%",
            nativeQuery = true)
    Page<Contest> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Redis 미사용 시: 단순 DB increment
    @Modifying
    @Query("UPDATE Contest c SET c.views = c.views + 1 WHERE c.id = :contestId")
    void increaseViews(@Param("contestId") Long contestId);

    // Redis 사용 시: 스케줄러가 누적값을 한 번에 반영
    @Modifying
    @Query("UPDATE Contest c SET c.views = c.views + :increment WHERE c.id = :contestId")
    void increaseViews(@Param("contestId") Long contestId, @Param("increment") Long increment);
}
