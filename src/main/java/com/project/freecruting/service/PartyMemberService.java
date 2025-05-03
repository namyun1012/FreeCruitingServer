package com.project.freecruting.service;

import com.project.freecruting.dto.party.*;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.User;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.repository.PartyRepository;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PartyMemberService {
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;

    // 처음 Party 만들 때만 사용되는 코드가 될 듯
    @Transactional
    public Long save(PartyMemberSaveRequestDto requestDto, Long user_id) {
        Long party_id = requestDto.getParty_id();

        User user = userRepository.findById(user_id).orElseThrow(() -> new RuntimeException("해당 USER 없음"));
        Party party = partyRepository.findById(party_id).orElseThrow(() -> new RuntimeException("해당 PARTY 없음"));;

        // 해당 Party 에 이미 당사자가 존재하는 경우 안 됨
        if(partyMemberRepository.findByPartyIdAndUserId(party_id, user_id).isPresent()) {
            throw new IllegalStateException("이미 참가한 유저입니다.");
        }

        // 해당 Party 의 Max Number Logic 을 확인하는 함수를 넣을 것
        if(party.getPartyMembers() != null && party.getPartyMembers().size() == party.getMax_number()) {
            throw new IllegalStateException("파티 인원 초과입니다.");
        }

        PartyMember result = partyMemberRepository.save(requestDto.toEntity(party, user));

        return result.getId();
    }

    @Transactional
    public Long update(Long id, PartyMemberUpdateRequestDto requestDto, Long user_id) {
        PartyMember partyMember = partyMemberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 PARTY MEMBER 없음. id=" + id));
        Long party_member_id = partyMember.getUser().getId();
        Long party_owner_id = partyMember.getParty().getOwner_id();

        
        // 자기 자신이 아니거나, 파티의 소유주가 아닌 경우 금지
        if (!party_member_id.equals(user_id) && !party_owner_id.equals(user_id)) {
            return 0L;
        }

        partyMember.update(requestDto.getParty_role());
        return id;
    }

    @Transactional
    public Long delete(Long id, Long user_id) {
        PartyMember partyMember = partyMemberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 PARTY MEMBER 없음. id=" + id));
        Long party_member_id = partyMember.getUser().getId();
        Long party_owner_id = partyMember.getParty().getOwner_id();

        // 자기 자신이 아니거나, 파티의 소유주가 아닌 경우 금지
        if (!party_member_id.equals(user_id) && !party_owner_id.equals(user_id)) {
            return 0L;
        }

        partyMemberRepository.delete(partyMember);

        // 더 이상 인원 수가 없는 경우에 party 삭제
        if(partyMemberRepository.findByPartyId(id).isEmpty()) {
            partyRepository.deleteById(id);
        }

        return id;
    }

    @Transactional(readOnly = true)
    public List<PartyMemberListResponseDto> findByPartyId(Long party_id) {
        return partyMemberRepository.findByPartyId(party_id).stream()
                .map(PartyMemberListResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PartyMemberListResponseDto> findByUserId(Long user_id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);


        return partyMemberRepository.findByUserId(user_id, pageable).map(PartyMemberListResponseDto::new);
    }

}
