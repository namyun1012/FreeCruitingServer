package com.project.freecruting.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    private String author;

    private String imageURL;

    private Long author_id;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Comment> comments;

    public enum PostType {
        PROJECT, STUDY, REVIEW, ANNOUNCEMENT;

        public static PostType fromString(String type) {
            for (PostType postType : PostType.values()) {
                if (postType.name().equalsIgnoreCase(type.trim())) {
                    return postType;
                }
            }
            throw new IllegalArgumentException("Unknown enum constant: " + type);
        }
    }

    @Builder
    public Post(String title, String content, String author, String imageURL, PostType type, Long author_id) {
        this.title      = title;
        this.content    = content;
        this.author     = author;
        this.imageURL   = imageURL;
        this.type       = type;
        this.author_id  = author_id;
    }

    public void update(String title, String content, String imageURL, PostType type) {
        this.title      = title;
        this.content    = content;
        this.imageURL   = imageURL;
        this.type       = type;
    }
}
