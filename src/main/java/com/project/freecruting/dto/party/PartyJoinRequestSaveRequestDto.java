package com.project.freecruting.dto.party;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyJoinRequest;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.RequestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartyJoinRequestSaveRequestDto {
    private Long party_id;
    private String party_role;

    @Builder
    public PartyJoinRequestSaveRequestDto(String party_role, Long party_id) {
        this.party_role = party_role;
        this.party_id = party_id;
    }

    public PartyJoinRequest toEntity(Party party, User user) {
        return PartyJoinRequest.builder()
                .party_role(party_role)
                .party(party)
                .user(user)
                .status(RequestStatus.PENDING)
                .build();
    }



}
