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
    
    // Google Oauth2User ID 사용
    private String author_id;

    // private Users Owner;
    @Builder
    public Post(String title, String content, String author, String imageURL, String type, String author_id) {
        this.title      = title;
        this.content    = content;
        this.author     = author;
        this.imageURL   = imageURL;
        this.type       = type;
        this.author_id  = author_id;
    }

    public void update(String title, String content, String imageURL, String type) {
        this.title      = title;
        this.content    = content;
        this.imageURL   = imageURL;
        this.type       = type;
    }
}
