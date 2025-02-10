package com.project.freecruting.config.auth.dto;

import com.project.freecruting.model.Users;
import lombok.Getter;

import java.io.Serializable;
@Getter
public class SessionUser implements Serializable {
    private Long   id;
    private String name;
    private String email;
    private String picture;

    public SessionUser(Users user) {
        this.id   = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.picture = user.getPicture();
    }
}