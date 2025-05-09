package com.project.freecruting.model;

import com.project.freecruting.model.type.RequestStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
public class PartyJoinRequest extends BaseTimeEntity{

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Party party;

    @ManyToOne
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private String party_role;

    @Builder
    public PartyJoinRequest(String party_role, Party party, User user, RequestStatus status) {
        this.party_role   = party_role;
        this.user         = user;
        this.party        = party;
        this.status       = status;
    }
}
