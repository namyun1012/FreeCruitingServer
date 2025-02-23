package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class Comment extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String author;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "users_id", nullable = false)
    private Users user;

    @Builder
    public Comment(String content, Post post, Users user, String author) {
        this.content    = content;
        this.post       = post;
        this.user       = user;
        this.author     = author;
    }

    public void update(String content) {
        this.content    = content;
    }
}
