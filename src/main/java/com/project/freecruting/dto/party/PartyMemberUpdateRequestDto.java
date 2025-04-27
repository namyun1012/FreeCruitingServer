package com.project.freecruting.dto.party;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartyMemberUpdateRequestDto {
    private String party_role;

    @Builder
    public PartyMemberUpdateRequestDto(String party_role) {
        this.party_role = party_role;

    }
}
