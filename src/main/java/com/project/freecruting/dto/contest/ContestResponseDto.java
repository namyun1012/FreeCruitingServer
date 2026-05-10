package com.project.freecruting.dto.contest;

import com.project.freecruting.model.Contest;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ContestResponseDto {

    private Long id;
    private String title;
    private String description;
    private String organizer;
    private String category;
    private LocalDate applicationStartDate;
    private LocalDate applicationDeadline;
    private LocalDate contestStartDate;
    private LocalDate contestEndDate;
    private String imageUrl;
    private String officialUrl;
    private String target;
    private String region;
    private String status;          // ContestStatus (날짜 기반 자동 계산)
    private int views;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    public ContestResponseDto(Contest entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.description = entity.getDescription();
        this.organizer = entity.getOrganizer();
        this.category = entity.getCategory().name();
        this.applicationStartDate = entity.getApplicationStartDate();
        this.applicationDeadline = entity.getApplicationDeadline();
        this.contestStartDate = entity.getContestStartDate();
        this.contestEndDate = entity.getContestEndDate();
        this.imageUrl = entity.getImageUrl();
        this.officialUrl = entity.getOfficialUrl();
        this.target = entity.getTarget();
        this.region = entity.getRegion();
        this.status = entity.getStatus().name();
        this.views = entity.getViews();
        this.createdDate = entity.getCreatedDate();
        this.modifiedDate = entity.getModifiedDate();
    }
}
