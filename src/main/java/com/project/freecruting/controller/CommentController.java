package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.comment.CommentSaveRequestDto;
import com.project.freecruting.dto.comment.CommentUpdateRequestDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.service.CommentService;
import com.project.freecruting.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    // Save 용도
    @PostMapping("/comment")
    public ResponseEntity<?> save(@RequestBody CommentSaveRequestDto requestDto, @LoginUser SessionUser user) {
        // requestDto 에서 User ID 는 Controller 에서 Setting 을 해주기.
        requestDto.setUser_id(user.getId());
        requestDto.setAuthor(user.getName());
        Long result = commentService.save(requestDto);

        if(result == 0L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "댓글 작성 실패"));
        }

        return ResponseEntity.ok(Map.of("message", "댓글 입력 완료"));
    }

    @PutMapping("/comment/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CommentUpdateRequestDto requestDto, @LoginUser SessionUser user) {
        Long author_id = user.getId();
        Long result = commentService.update(id, requestDto, author_id);

        if(result == 0L) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "작성자만 수정할 수 있습니다."));
        }

        return ResponseEntity.ok(Map.of("message", "Update successful"));
    }

    @DeleteMapping("/comment/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @LoginUser SessionUser user) {
        Long author_id = user.getId();
        Long result = commentService.delete(id, author_id);

        if(result == 0L) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "작성자만 삭제할 수 있습니다."));
        }

        return ResponseEntity.ok(Map.of("message", "Delete successful"));
    }
}
