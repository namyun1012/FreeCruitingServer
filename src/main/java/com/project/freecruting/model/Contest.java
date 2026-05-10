package com.project.freecruting.model;

import com.project.freecruting.model.type.ContestCategory;
import com.project.freecruting.model.type.ContestStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
public class Contest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String title;                       // 공모전 명

    @Column(columnDefinition = "TEXT")
    private String description;                 // 상세 설명

    @Column(nullable = false)
    private String organizer;                   // 주최기관

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestCategory category;           // 분야

    @Column(nullable = false)
    private LocalDate applicationStartDate;     // 접수 시작일

    @Column(nullable = false)
    private LocalDate applicationDeadline;      // 접수 마감일

    private LocalDate contestStartDate;         // 대회 시작일 (선택)

    private LocalDate contestEndDate;           // 대회 종료일 (선택)

    private String imageUrl;                    // 배너 이미지

    @Column(length = 1000)
    private String officialUrl;                 // 공식 사이트 링크

    private String target;                      // 참가 대상 (ex. "대학(원)생", "누구나")

    private String region;                      // 지역 (ex. "온라인", "서울")

    private int views;

    @Builder
    public Contest(String title, String description, String organizer, ContestCategory category,
                   LocalDate applicationStartDate, LocalDate applicationDeadline,
                   LocalDate contestStartDate, LocalDate contestEndDate,
                   String imageUrl, String officialUrl, String target, String region) {
        this.title = title;
        this.description = description;
        this.organizer = organizer;
        this.category = category;
        this.applicationStartDate = applicationStartDate;
        this.applicationDeadline = applicationDeadline;
        this.contestStartDate = contestStartDate;
        this.contestEndDate = contestEndDate;
        this.imageUrl = imageUrl;
        this.officialUrl = officialUrl;
        this.target = target;
        this.region = region;
    }

    public void update(String title, String description, String organizer, ContestCategory category,
                       LocalDate applicationStartDate, LocalDate applicationDeadline,
                       LocalDate contestStartDate, LocalDate contestEndDate,
                       String imageUrl, String officialUrl, String target, String region) {
        this.title = title;
        this.description = description;
        this.organizer = organizer;
        this.category = category;
        this.applicationStartDate = applicationStartDate;
        this.applicationDeadline = applicationDeadline;
        this.contestStartDate = contestStartDate;
        this.contestEndDate = contestEndDate;
        this.imageUrl = imageUrl;
        this.officialUrl = officialUrl;
        this.target = target;
        this.region = region;
    }

    // DB에 저장하지 않고 날짜 기반으로 자동 계산
    public ContestStatus getStatus() {
        LocalDate today = LocalDate.now();
        if (today.isBefore(applicationStartDate))                          return ContestStatus.UPCOMING;
        if (!today.isAfter(applicationDeadline))                           return ContestStatus.RECRUITING;
        if (contestEndDate != null && !today.isAfter(contestEndDate))      return ContestStatus.IN_PROGRESS;
        return ContestStatus.CLOSED;
    }

    public void increaseViews() {
        this.views++;
    }
}
