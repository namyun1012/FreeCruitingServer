package com.project.freecruting.dto.contest;

import com.project.freecruting.model.Contest;
import com.project.freecruting.model.type.ContestCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ContestSaveRequestDto {

    private String title;
    private String description;
    private String organizer;
    private String category;            // ContestCategory enum name (ex. "PROGRAMMING")
    private LocalDate applicationStartDate;
    private LocalDate applicationDeadline;
    private LocalDate contestStartDate; // nullable
    private LocalDate contestEndDate;   // nullable
    private String imageUrl;
    private String officialUrl;
    private String target;
    private String region;

    @Builder
    public ContestSaveRequestDto(String title, String description, String organizer, String category,
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

    public Contest toEntity() {
        return Contest.builder()
                .title(title)
                .description(description)
                .organizer(organizer)
                .category(ContestCategory.valueOf(category.toUpperCase()))
                .applicationStartDate(applicationStartDate)
                .applicationDeadline(applicationDeadline)
                .contestStartDate(contestStartDate)
                .contestEndDate(contestEndDate)
                .imageUrl(imageUrl)
                .officialUrl(officialUrl)
                .target(target)
                .region(region)
                .build();
    }
}
