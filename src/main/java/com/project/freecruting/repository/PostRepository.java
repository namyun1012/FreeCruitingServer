package com.project.freecruting.repository;

import com.project.freecruting.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p ORDER BY p.id DESC")
    List<Post> findAllDesc();

    @Query("SELECT p FROM Post p Where p.type = :type ORDER BY p.id DESC")
    List<Post> findByType(@Param("type") Post.PostType type);


    Page<Post> findByTitle(String title, Pageable pageable);
    Page<Post> findByContent(String content, Pageable pageable);

    Page<Post> findByAuthor(String author, Pageable pageable);

    Page<Post> findByTitleOrContentOrAuthor(String title, String content, String author, Pageable pageable);

    // Page 처리
    Page<Post> findAll(Pageable pageable);
    Page<Post> findAllByOrderByModifiedDateDesc(Pageable pageable);

    Page<Post> findByTypeOrderByModifiedDateDesc(Pageable pageable, Post.PostType type);

    @Modifying
    @Query("UPDATE Post p SET p.views = p.views + 1 WHERE p.id = :postId")
    void increaseViews(@Param("postId") Long postId);
}
