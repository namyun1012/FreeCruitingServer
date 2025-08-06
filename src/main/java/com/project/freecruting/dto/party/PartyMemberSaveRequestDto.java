package com.project.freecruting.dto.party;

import com.project.freecruting.model.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartyMemberSaveRequestDto {
    private String party_role;
    private Long party_id;
    @Builder
    public PartyMemberSaveRequestDto(String party_role) {
        this.party_role = party_role;
    }

    @Builder
    public PartyMemberSaveRequestDto(String party_role, Long party_id) {
        this.party_role = party_role;
        this.party_id = party_id;
    }

    public PartyMember toEntity(Party party, User user) {
        return PartyMember.builder()
                .party_role(party_role)
                .party(party)
                .user(user)
                .build();
    }
}
