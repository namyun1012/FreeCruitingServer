package com.project.freecruting.repository;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyRepository extends JpaRepository<Party, Long> {

    Page<Party> findAll(Pageable pageable);

    @Modifying
    @Query("UPDATE Party p SET p.owner_id = :newOwnerId WHERE p.id = :partyId")
    int updateOwnerIdById(@Param("newOwnerId") Long newOwnerId, @Param("partyId") long partyId);
}
