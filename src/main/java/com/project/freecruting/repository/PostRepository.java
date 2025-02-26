package com.project.freecruting.repository;

import com.project.freecruting.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT p FROM Post p WHERE p.title LIKE CONCAT('%', :query, '%')")
    List<Post> searchByTitle(@Param("query") String query);

    @Query("SELECT p FROM Post p WHERE p.content LIKE CONCAT('%', :query, '%')")
    List<Post> searchByContent(@Param("query") String query);

    @Query("SELECT p FROM Post p WHERE p.author LIKE CONCAT('%', :query, '%')")
    List<Post> searchByAuthor(@Param("query") String query);

    // JPQL 에서는 UNION 이 존재하지 않아서 native query 로 처리함.
    @Query(value = "SELECT * FROM post p WHERE p.title LIKE CONCAT('%', :query, '%')" +
            "UNION SELECT * FROM post p WHERE p.content LIKE CONCAT('%', :query, '%')" +
            "UNION SELECT * FROM post p WHERE p.author LIKE CONCAT('%', :query, '%')",
            nativeQuery = true)
    List<Post> searchByAll(@Param("query") String query);

    // Page 처리
    Page<Post> findAll(Pageable pageable);
    Page<Post> findAllByOrderByModifiedDateDesc(Pageable pageable);
}
