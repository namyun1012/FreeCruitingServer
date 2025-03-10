package com.project.freecruting.repository;

import com.project.freecruting.model.Comment;
import com.project.freecruting.model.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    Optional<PartyMember> findByPartyIdAndUserId(Long partyId, Long userId);

    List<PartyMember> findByPartyId(Long partyId);
    List<PartyMember> findByUserId(Long userId);
}
