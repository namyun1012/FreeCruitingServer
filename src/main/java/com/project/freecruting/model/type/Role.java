package com.project.freecruting.model.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    GUEST("ROLE_GUEST", "GUEST"),
    USER("ROLE_USER", "USER");

    private final String key;
    private final String title;
}
