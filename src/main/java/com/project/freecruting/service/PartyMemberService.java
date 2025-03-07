package com.project.freecruting.service;

import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberUpdateRequestDto;
import com.project.freecruting.dto.party.PartySaveRequestDto;
import com.project.freecruting.dto.party.PartyUpdateRequestDto;
import com.project.freecruting.model.Party;
import com.project.freecruting.model.PartyMember;
import com.project.freecruting.model.Users;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.repository.PartyRepository;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PartyMemberService {
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;

    @Transactional
    public Long save(PartyMemberSaveRequestDto requestDto) {
        Long user_id = requestDto.getUser_id();
        Long party_id = requestDto.getParty_id();

        Users user = userRepository.findById(user_id).orElseThrow(() -> new RuntimeException("해당 USER 없음"));
        Party party = partyRepository.findById(party_id).orElseThrow(() -> new RuntimeException("해당 USER 없음"));;

        PartyMember result = partyMemberRepository.save(requestDto.toEntity(party, user));

        // party Member 자기 자신 추가는 Controller 에서 각각 호출하는 것으로
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
        return id;

    }

}
