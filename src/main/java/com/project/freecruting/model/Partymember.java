package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class Partymember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // private Users user;

    // private Party party;

    @Enumerated(EnumType.STRING)
    private Party_role party_role;

    public enum Party_role {
        LEADER,
        MEMBER
    }
}
