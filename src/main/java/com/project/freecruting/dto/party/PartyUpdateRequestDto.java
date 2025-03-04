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
    @Builder
    public PartyUpdateRequestDto(String name, String description) {
        this.name = name;
        this.description = description;
    }

}
