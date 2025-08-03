package com.project.freecruting.controller;


import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberUpdateRequestDto;
import com.project.freecruting.dto.party.PartySaveRequestDto;
import com.project.freecruting.repository.PartyMemberRepository;
import com.project.freecruting.service.PartyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PartyMemberController {
    private final PartyMemberService partyMemberService;

    // 추후에는 Party Owner 의 승인이 이루어져야 이 API 가 호출되도록 바꿀 필요 있음
    @Transactional
    @PostMapping("/partymembers")
    public ResponseEntity<?> save(@RequestBody PartyMemberSaveRequestDto requestDto, @LoginUser SessionUser user) {
        // requestDto 에서 User ID 는 Controller 에서 Setting 을 해주기.
        Long user_id = user.getId();

        // Party Member 를 생성하는 과정
        Long result_id = partyMemberService.save(requestDto, user_id);
        if(result_id == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파티 멤버 생성 실패"));
        }
        
        return ResponseEntity.ok(Map.of("message", "파티 멤버 가입 완료"));
    }

    @Transactional
    @PutMapping("/partymembers/{id}")
    public ResponseEntity<?> update(@PathVariable Long id , @RequestBody PartyMemberUpdateRequestDto requestDto, @LoginUser SessionUser user) {
        Long user_id = user.getId();
        Long result_id = partyMemberService.update(id, requestDto, user_id);

        if(result_id == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파티 멤버 수정 실패"));
        }

        return ResponseEntity.ok(Map.of("message", "파티 멤버 수정 완료"));
    }

    @Transactional
    @DeleteMapping("partymembers/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @LoginUser SessionUser user) {
        Long user_id = user.getId();
        Long result_id = partyMemberService.delete(id, user_id);

        if(result_id == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파티 멤버 삭제 실패"));
        }

        return ResponseEntity.ok(Map.of("message", "파티 멤버 삭제 완료"));
    }


}
