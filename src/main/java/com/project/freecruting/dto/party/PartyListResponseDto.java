package com.project.freecruting.dto.party;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PartyListResponseDto {
    private Long id;
    private String name;
    private String description;
    private int max_number;
    public PartyListResponseDto(Party entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.max_number = entity.getMax_number();
    }
}
