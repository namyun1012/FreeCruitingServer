package com.project.freecruting.dto.contest;

import com.project.freecruting.model.Contest;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ContestListResponseDto {

    private Long id;
    private String title;
    private String organizer;
    private String category;
    private LocalDate applicationDeadline;
    private String imageUrl;
    private String target;
    private String region;
    private String status;
    private int views;
    private LocalDateTime modifiedDate;

    public ContestListResponseDto(Contest entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.organizer = entity.getOrganizer();
        this.category = entity.getCategory().name();
        this.applicationDeadline = entity.getApplicationDeadline();
        this.imageUrl = entity.getImageUrl();
        this.target = entity.getTarget();
        this.region = entity.getRegion();
        this.status = entity.getStatus().name();
        this.views = entity.getViews();
        this.modifiedDate = entity.getModifiedDate();
    }
}
