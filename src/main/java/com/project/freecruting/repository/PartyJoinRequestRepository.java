package com.project.freecruting.repository;

import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyJoinRequest;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.type.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyJoinRequestRepository extends JpaRepository<PartyJoinRequest, Long> {

    Optional<PartyJoinRequest> findByPartyIdAndUserId(Long partyId, Long userId);

    Optional<PartyJoinRequest> findByPartyIdAndUserIdAndStatus(Long partyId, Long userId, RequestStatus status);
    List<PartyJoinRequest> findByPartyIdAndStatus(Long partyId, RequestStatus status);

    @Modifying
    @Query("UPDATE PartyJoinRequest p SET p.status = :status  WHERE p.id = :id")
    void setStatus(@Param("id") Long Id, @Param("status")RequestStatus status);

}
