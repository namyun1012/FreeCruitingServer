package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.comment.CommentSaveRequestDto;
import com.project.freecruting.dto.party.PartyMemberSaveRequestDto;
import com.project.freecruting.dto.party.PartySaveRequestDto;
import com.project.freecruting.dto.party.PartyUpdateRequestDto;
import com.project.freecruting.model.ChatMessage;
import com.project.freecruting.service.ChatService;
import com.project.freecruting.service.PartyMemberService;
import com.project.freecruting.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PartyController {

    private final PartyService partyService;
    private final PartyMemberService partyMemberService;
    private final ChatService chatService;
    
    
    // 코드 개선 필수
    @Transactional
    @PostMapping("/partys")
    public ResponseEntity<?> save(@RequestBody PartySaveRequestDto requestDto, @LoginUser SessionUser user) {
        // requestDto 에서 User ID 는 Controller 에서 Setting 을 해주기.
        Long user_id = user.getId();

        // Party 를 생성하는 과정
        Long party_id = partyService.save(requestDto, user_id);

        return ResponseEntity.ok(Map.of("message", "파티 생성 완료"));
    }

    @PutMapping("/partys/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PartyUpdateRequestDto requestDto, @LoginUser SessionUser user) {
        Long user_id = user.getId();
        Long result_id = partyService.update(id, requestDto, user_id);


        return ResponseEntity.ok(Map.of("message", "파티 수정 완료"));
    }

    @DeleteMapping("/partys/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @LoginUser SessionUser user) {
        Long user_id = user.getId();
        Long result_id = partyService.delete(id, user_id);

        return ResponseEntity.ok(Map.of("message", "파티 삭제 완료"));
        
    }

    @GetMapping("/chat/rooms/{roomId}/messages")
    public ResponseEntity<?> getChatMessagesByRoomId(@PathVariable Long roomId) {
        List<ChatMessage> messages = chatService.getChatMessagesByRoomId(roomId);
        return ResponseEntity.ok(messages);
    }
}
