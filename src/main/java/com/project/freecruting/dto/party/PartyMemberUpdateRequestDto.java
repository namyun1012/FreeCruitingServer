package com.project.freecruting.dto.party;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.Users;
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
