package com.project.freecruting.dto.party;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class PartyResponseDto {
    private Long id;
    private String name;
    private String description;
    private List<PartyMember> partyMembers;
    private int max_number;

    public PartyResponseDto(Party entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.partyMembers = entity.getPartyMembers();
        this.max_number = entity.getMax_number();
    }
}
