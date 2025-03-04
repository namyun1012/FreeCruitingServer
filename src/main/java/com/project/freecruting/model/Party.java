package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class Party extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyMember> partyMembers;

    // USER_ID
    private Long owner_id;

    @Builder
    public Party(String name, String description, Long owner_id) {
        this.name = name;
        this.description = description;
        this.owner_id = owner_id;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }


}
