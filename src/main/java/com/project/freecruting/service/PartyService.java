package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentUpdateRequestDto;
import com.project.freecruting.dto.party.*;
import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.*;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.repository.PartyRepository;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PartyService {
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    // Party 생성 시 무조건 자기 자신을 Party Member 로 추가
    @Transactional
    public Long save(PartySaveRequestDto requestDto, Long user_id) {
        requestDto.setOwner_id(user_id);
        Party party = partyRepository.save(requestDto.toEntity());
        User user = userRepository.findById(user_id).orElseThrow(() -> new NotFoundException("해당 USER 없음. id=" + user_id));;
        // party Member 자기 자신 추가는 Controller 에서 각각 호출하는 것으로
        // 자기 자신을 owner 로 party member 로 추가하는 단계
        PartyMemberSaveRequestDto partyMemberSaveRequestDto = new PartyMemberSaveRequestDto("owner");
        partyMemberRepository.save(partyMemberSaveRequestDto.toEntity(party, user));

        return party.getId();
    }

    @Transactional
    public Long update(Long id, PartyUpdateRequestDto requestDto, Long user_id) {
        Party party = partyRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 PARTY 없음. id=" + id));
        Long party_owner_id = party.getOwner_id();

        if (!party_owner_id.equals(user_id)) {
            throw new ForbiddenException("해당 파티의 오너가 아닙니다");
        }

        party.update(requestDto.getName(), requestDto.getDescription(), requestDto.getMax_number());
        return id;
    }

    @Transactional
    public Long delete(Long id, Long user_id) {
        Party party = partyRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 PARTY 없음. id = " + id));

        Long party_owner_id = party.getOwner_id();

        if (!party_owner_id.equals(user_id)) {
            throw new ForbiddenException("해당 파티의 오너가 아닙니다");
        }

        partyRepository.delete(party);
        return id;
    }

    @Transactional(readOnly = true)
    public Page<PartyListResponseDto> findAllPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return partyRepository.findAll(pageable)
                .map(PartyListResponseDto::new);
    }

    @Transactional(readOnly = true)
    public PartyResponseDto findById(Long id) {
        Party entity = partyRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 PARTY 없음. id=" + id));
        return new PartyResponseDto(entity);
    }

    @Transactional(readOnly = true)
    public PartyResponseDto findByIdForOwner(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new NotFoundException("해당 PARTY 없음. id=" + partyId));

        if(!party.getOwner_id().equals(userId)) {
            throw new ForbiddenException("Party Owner 만 접근 가능합니다.");
        }

        return new PartyResponseDto(party);
    }

    @Transactional(readOnly = true)
    public PartyResponseDto findByIdForMember(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new NotFoundException("해당 PARTY 없음. id=" + partyId));

        PartyMember partyMember = partyMemberRepository.findByPartyIdAndUserId(partyId, userId).orElseThrow(() ->
                new ForbiddenException("해당 파티의 구성원만 접근 가능합니다"));

        return new PartyResponseDto(party);
    }
}
