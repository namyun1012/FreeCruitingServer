package com.project.freecruting.dto.party;

import com.project.freecruting.model.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class PartyMemberSaveRequestDto {
    private Long party_id;
    @Builder
    public PartyMemberSaveRequestDto(String party_role, Long party_id) {
        this.party_id = party_id;
    }

    public PartyMember toEntity(Party party, Users user) {
        return PartyMember.builder()
                .party_role("MEMBER")
                .party(party)
                .user(user)
                .build();
    }
}
