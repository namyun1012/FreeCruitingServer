package com.project.freecruting.repository;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    Optional<PartyMember> findByPartyIdAndUserId(Long partyId, Long userId);

    List<PartyMember> findByPartyId(Long partyId);
    Page<PartyMember> findByUserId(Long userId, Pageable pageable);

    @Query(value = "SELECT * FROM party_member WHERE party_id = :partyId LIMIT 1", nativeQuery = true)
    Optional<PartyMember> findRandomPartyMemberByPartyId(@Param("partyId") Long partyId);

    @Query(value = "SELECT COUNT(pm) > 0 FROM party_member pm WHERE pm.party_id = :partyId", nativeQuery = true)
    boolean existsByPartyId(@Param("partyId") Long partyId);


}
