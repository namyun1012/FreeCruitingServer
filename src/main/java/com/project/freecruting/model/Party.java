package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    String name;

    //@OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    //private List<Partymember> partyMembers = new ArrayList<>();

    // createdAt, updatedAt 추가 필요



}
