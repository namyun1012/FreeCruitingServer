package com.project.freecruting.dto.party;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartyMemberListResponseDto {
    private Long id;
    private String party_role;
    private Long party_id; // 추후에 수정 필요할 지도
    private Long user_id;
    private String user_name;
    private String party_name;
    public PartyMemberListResponseDto(PartyMember entity) {
        this.id = entity.getId();
        this.party_role = entity.getParty_role();
        this.party_id = entity.getParty().getId();
        this.user_id = entity.getUser().getId();
        this.user_name = entity.getUser().getName();
        this.party_name = entity.getParty().getName();
    }
}
