package com.project.freecruting.dto.party;

import com.project.freecruting.model.Party;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartyUpdateRequestDto {
    private String name;
    private String description;
    private int max_number;
    @Builder
    public PartyUpdateRequestDto(String name, String description, int max_number) {
        this.name = name;
        this.description = description;
        this.max_number = max_number;
    }

}
