package com.project.freecruting.service;

import com.project.freecruting.dto.party.PartyJoinRequestListResponseDto;
import com.project.freecruting.dto.party.PartyJoinRequestSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberListResponseDto;
import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyJoinRequest;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.RequestStatus;
import com.project.freecruting.repository.PartyJoinRequestRepository;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.repository.PartyRepository;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PartyJoinRequestService {
    private final PartyJoinRequestRepository partyJoinRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;

    @Transactional
    public Long save(PartyJoinRequestSaveRequestDto requestDto, Long user_id) {
        Long party_id = requestDto.getParty_id();

        User user = userRepository.findById(user_id).orElseThrow(() -> new NotFoundException("해당 USER 없음"));
        Party party = partyRepository.findById(party_id).orElseThrow(() -> new NotFoundException("해당 PARTY 없음"));;

        // 이미 신청을 진행했던 경우
        if(partyJoinRequestRepository.findByPartyIdAndUserIdAndStatus(party_id, user_id, RequestStatus.PENDING).isPresent()) {
            throw new InvalidStateException("이미 신청했습니다.");
        }

        // Party 에 이미 존재하는 경우 걸러냄
        if (partyMemberRepository.findByPartyIdAndUserId(party_id, user_id).isPresent()) {
            throw new InvalidStateException("이미 가입한 신청자 입니다.");
        }

        // 해당 Party 의 Max Number Logic 을 확인하는 함수를 넣을 것
        if (party.getPartyMembers() != null && party.getPartyMembers().size() == party.getMax_number()) {
            throw new InvalidStateException("파티 인원 초과입니다.");
        }

        PartyJoinRequest result = partyJoinRequestRepository.save(requestDto.toEntity(party, user));

        return result.getId();
    }

    @Transactional
    public void approve(Long request_id, Long owner_id) {
        PartyJoinRequest request = partyJoinRequestRepository.findById(request_id).orElseThrow(
                () -> new NotFoundException("해당 Party Join Request 없음"));

        Party party = request.getParty();

        if(!party.getOwner_id().equals(owner_id)) {
            throw new ForbiddenException("owner 만 승인할 수 있습니다.");
        }

        if(request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateException("이미 처리된 요청입니다.");
        }

        long cur_party_members_count = partyMemberRepository.findByPartyId(party.getId()).size();

        if(cur_party_members_count>= party.getMax_number()) {
            throw new InvalidStateException("이미 꽉 찬 상태입니다.");
        }

        partyJoinRequestRepository.setStatus(request_id, RequestStatus.APPROVED);
        // Party Member 생성할 때 partyMember service 호출 안하고 그대로 생성?
        partyMemberRepository.save(
                new PartyMemberSaveRequestDto(request.getParty_role(), request.getParty().getId()).toEntity(party, request.getUser()));
    }

    @Transactional
    public void reject(Long request_id, Long owner_id) {
        PartyJoinRequest request = partyJoinRequestRepository.findById(request_id).orElseThrow(
                () -> new NotFoundException("해당 Party Join Request 없음"));

        Party party = request.getParty();

        if(!party.getOwner_id().equals(owner_id)) {
            throw new ForbiddenException("owner 만 거부할 수 있습니다.");
        }

        if(request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateException("이미 처리된 요청입니다.");
        }

        partyJoinRequestRepository.setStatus(request_id, RequestStatus.REJECTED);
    }

    public List<PartyJoinRequestListResponseDto> findPendingByPartyId(Long party_id) {
        return partyJoinRequestRepository.findByPartyIdAndStatus(party_id, RequestStatus.PENDING).stream()
                .map(PartyJoinRequestListResponseDto::new)
                .collect(Collectors.toList());
    }
}
