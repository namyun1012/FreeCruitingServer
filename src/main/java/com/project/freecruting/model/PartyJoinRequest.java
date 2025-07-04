package com.project.freecruting.model;

import com.project.freecruting.model.type.RequestStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;

@Getter
@NoArgsConstructor
@Entity
public class PartyJoinRequest extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", foreignKey = @ForeignKey(name = "fk_party_join_request_party"))
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
