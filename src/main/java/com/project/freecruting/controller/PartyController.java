package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.comment.CommentSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.dto.party.PartySaveRequestDto;
import com.project.freecruting.service.PartyMemberService;
import com.project.freecruting.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PartyController {

    private final PartyService partyService;
    private final PartyMemberService partyMemberService;

    @Transactional
    @PostMapping("/party")
    public ResponseEntity<?> save(@RequestBody PartySaveRequestDto requestDto, @LoginUser SessionUser user) {
        // requestDto 에서 User ID 는 Controller 에서 Setting 을 해주기.
        Long user_id = user.getId();

        requestDto.setOwner_id(user_id);
        Long party_id = partyService.save(requestDto);

        if(party_id == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파티 생성 실패"));
        }
        
        // 자기 자신을 owner 로 party member 로 추가하는 단계
        PartyMemberSaveRequestDto partyMemberSaveRequestDto = new PartyMemberSaveRequestDto("owner", party_id, user_id);
        Long party_member_id = partyMemberService.save(partyMemberSaveRequestDto);

        if(party_member_id == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파티에 자기 자신 추가 실패"));
        }
        
        return ResponseEntity.ok(Map.of("message", "파티 생성 완료"));
    }
}
