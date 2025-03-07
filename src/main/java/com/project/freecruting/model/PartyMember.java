package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class PartyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "users_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    // 그냥 자유로 설정 가능하게
    private String party_role;

    @Builder
    public PartyMember(String party_role, Party party, Users user) {
        this.party_role   = party_role;
        this.user         = user;
        this.party        = party;
    }

    public void update(String party_role) {
        this.party_role = party_role;
    }
}
