package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

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
    @Column(nullable = false)
    private Long owner_id;

    @Column(nullable = false)
    @Check(constraints = "max_number >= 5 AND max_number <= 15")
    private int max_number;

    @Builder
    public Party(String name, String description, Long owner_id, int max_number) {
        this.name = name;
        this.description = description;
        this.owner_id = owner_id;
        this.max_number = max_number;
    }

    public void update(String name, String description, int max_number) {
        this.name           = name;
        this.description    = description;
        this.max_number     = max_number;
    }


}
