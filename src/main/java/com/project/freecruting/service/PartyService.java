package com.project.freecruting.service;

import com.project.freecruting.dto.comment.CommentUpdateRequestDto;
import com.project.freecruting.dto.party.PartyListResponseDto;
import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.dto.party.PartySaveRequestDto;
import com.project.freecruting.dto.party.PartyUpdateRequestDto;
import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.model.Comment;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.Post;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PartyService {
    private final PartyRepository partyRepository;
    // Party 생성 시 자기 자신을 Party Member 로 추가
    @Transactional
    public Long save(PartySaveRequestDto requestDto) {
        Party party = partyRepository.save(requestDto.toEntity());
        // party Member 자기 자신 추가는 Controller 에서 각각 호출하는 것으로
        return party.getId();
    }

    @Transactional
    public Long update(Long id, PartyUpdateRequestDto requestDto, Long user_id) {
        Party party = partyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 PARTY 없음. id=" + id));
        Long party_owner_id = party.getOwner_id();

        if (!party_owner_id.equals(user_id)) {
            return 0L;
        }

        party.update(requestDto.getName(), requestDto.getDescription(), requestDto.getMax_number());
        return id;
    }

    @Transactional
    public Long delete(Long id, Long user_id) {
        Party party = partyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 PARTY 없음. id = " + id));

        Long party_owner_id = party.getOwner_id();

        if (!party_owner_id.equals(user_id)) {
            return 0L;
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
}
