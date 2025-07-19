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

    @Query(value = "SELECT * FROM post p WHERE p.title LIKE %:title%",
            countQuery = "SELECT count(*) FROM post p WHERE p.title LIKE %:title%",
            nativeQuery = true)
    Page<Post> findByTitle(@Param("title") String title, Pageable pageable);


    @Query(value = "SELECT * FROM post p WHERE p.content LIKE %:content%",
            countQuery = "SELECT count(*) FROM post p WHERE p.content LIKE %:content%",
            nativeQuery = true)
    Page<Post> findByContent(@Param("content") String content, Pageable pageable);

    @Query(value = "SELECT * FROM post p WHERE p.author LIKE %:author%",
            countQuery = "SELECT count(*) FROM post p WHERE p.author LIKE %:author%",
            nativeQuery = true)
    Page<Post> findByAuthor(@Param("author") String author, Pageable pageable);

    @Query(value = "SELECT * FROM post p WHERE " +
            "p.title LIKE %:keyword% OR " +
            "p.content LIKE %:keyword% OR " +
            "p.author LIKE %:keyword%",
            countQuery = "SELECT count(*) FROM post p WHERE " +
                    "p.title LIKE %:keyword% OR " +
                    "p.content LIKE %:keyword% OR " +
                    "p.author LIKE %:keyword%",
            nativeQuery = true)
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Page 처리
    Page<Post> findAll(Pageable pageable);
    Page<Post> findAllByOrderByModifiedDateDesc(Pageable pageable);

    Page<Post> findAllByOrderByIdDesc(Pageable pageable);
    Page<Post> findByTypeOrderByIdDesc(Pageable pageable, Post.PostType type);
    Page<Post> findByTypeOrderByModifiedDateDesc(Pageable pageable, Post.PostType type);

    @Modifying
    @Query("UPDATE Post p SET p.views = p.views + 1 WHERE p.id = :postId")
    void increaseViews(@Param("postId") Long postId);

    @Modifying
    @Query("update Post p SET p.views = p.views + :increment where p.id = :postId")
    void increaseViews(@Param("postId") Long postId, @Param("increment") Long increment);

}
