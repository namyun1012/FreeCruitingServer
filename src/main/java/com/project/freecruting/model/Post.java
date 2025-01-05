package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String type;

    private String author;
    private String imageURL;

    @Builder
    public Post(String title, String content, String author, String imageURL, String type) {
        this.title      = title;
        this.content    = content;
        this.author     = author;
        this.imageURL   = imageURL;
        this.type       = type;
    }

    public void update(String title, String content, String imageURL, String type) {
        this.title      = title;
        this.content    = content;
        this.imageURL   = imageURL;
        this.type       = type;

    }
}
