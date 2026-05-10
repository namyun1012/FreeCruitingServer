package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.contest.ContestListResponseDto;
import com.project.freecruting.dto.contest.ContestResponseDto;
import com.project.freecruting.dto.contest.ContestSaveRequestDto;
import com.project.freecruting.dto.contest.ContestUpdateRequestDto;
import com.project.freecruting.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/contests")
public class ContestController {

    private final ContestService contestService;

    // 공모전 등록 (관리자 전용 — TODO: ADMIN 롤 적용 예정)
    @PostMapping
    public ResponseEntity<?> save(@RequestBody ContestSaveRequestDto requestDto) {
        Long id = contestService.save(requestDto);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // 공모전 수정 (관리자 전용)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody ContestUpdateRequestDto requestDto) {
        contestService.update(id, requestDto);
        return ResponseEntity.ok(Map.of("message", "Update successful"));
    }

    // 공모전 삭제 (관리자 전용)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        contestService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Delete successful"));
    }

    // 공모전 단건 조회
    @GetMapping("/{id}")
    public ContestResponseDto findById(@PathVariable Long id,
                                       @LoginUser SessionUser user) {
        Long userId = (user != null) ? user.getId() : null;
        return contestService.findById(id, userId);
    }

    // 공모전 목록 조회 — category 필터 또는 keyword 검색
    @GetMapping
    public ResponseEntity<Page<ContestListResponseDto>> getContests(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ContestListResponseDto> result = contestService.findContestPages(category, keyword, page, size);
        return ResponseEntity.ok(result);
    }
}
