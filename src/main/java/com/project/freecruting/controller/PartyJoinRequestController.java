package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.party.PartyJoinRequestSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.service.PartyJoinRequestService;
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
public class PartyJoinRequestController {
    private final PartyJoinRequestService partyJoinRequestService;
    private final PartyMemberService partyMemberService;

    @Transactional
    @PostMapping("/party_join_requests")
    public ResponseEntity<?> save(@RequestBody PartyJoinRequestSaveRequestDto requestDto, @LoginUser SessionUser user) {
        // requestDto 에서 User ID 는 Controller 에서 Setting 을 해주기.
        Long user_id = user.getId();

        // Party Member 를 생성하는 과정
        Long result_id = partyJoinRequestService.save(requestDto, user_id);

        if(result_id == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파티 멤버 생성 실패"));
        }

        return ResponseEntity.ok(Map.of("message", "파티 신청 완료"));
    }

    @Transactional
    @PostMapping("/party_join_requests/{request_id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long request_id, @LoginUser SessionUser user) {

        Long user_id = user.getId();


        partyJoinRequestService.approve(request_id, user_id);
        return ResponseEntity.ok().body("요청 승인");

    }

    @Transactional
    @PostMapping("/party_join_requests/{request_id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long request_id, @LoginUser SessionUser user) {

        Long user_id = user.getId();

        partyJoinRequestService.reject(request_id, user_id);
        return ResponseEntity.ok().body("요청 거부");

    }
}
