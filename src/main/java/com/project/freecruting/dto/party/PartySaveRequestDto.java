package com.project.freecruting.dto.party;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class PartySaveRequestDto {

    private String name;
    private String description;


    @Setter
    private Long owner_id;

    @Builder
    public PartySaveRequestDto(String name, String description, Long owner_id) {
        this.name = name;
        this.description = description;
        this.owner_id = owner_id;
    }

    public Party toEntity() {
        return Party.builder()
                .name(name)
                .description(description)
                .owner_id(owner_id)
                .build();
    }
}
