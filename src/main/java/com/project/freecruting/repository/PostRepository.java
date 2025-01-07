package com.project.freecruting.repository;

import com.project.freecruting.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p ORDER BY p.id DESC")
    List<Post> findAllDesc();

    @Query("SELECT p FROM Post p Where p.type = :type ORDER BY p.id DESC")
    List<Post> findByType(@Param("type") String type);
}
