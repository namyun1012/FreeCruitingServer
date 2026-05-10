package com.project.freecruting.dto.contest;

import com.project.freecruting.model.type.ContestCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ContestUpdateRequestDto {

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

    @Builder
    public ContestUpdateRequestDto(String title, String description, String organizer, String category,
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

    public ContestCategory parsedCategory() {
        return ContestCategory.valueOf(category.toUpperCase());
    }
}
