package com.project.freecruting.repository;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {

    Page<Party> findAll(Pageable pageable);
}
