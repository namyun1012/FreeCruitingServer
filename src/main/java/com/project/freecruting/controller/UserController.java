package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.user.UserSaveRequestDto;
import com.project.freecruting.dto.user.UserUpdateRequestDto;
import com.project.freecruting.model.User;
import com.project.freecruting.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final HttpSession httpSession;

    @PostMapping("/user")
    public ResponseEntity<?> save(@Valid @RequestBody UserSaveRequestDto requestDto) {

        Long result = userService.save(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "save successful"));


    }

    @PutMapping("/user")
    public ResponseEntity<?> update(@RequestBody UserUpdateRequestDto requestDto, @LoginUser SessionUser sessionUser) {
        String email = sessionUser.getEmail();
        User user = userService.update(requestDto, email);

        if (user != null) {
            // Session Update 적용해 주어야 한다.
            httpSession.setAttribute("user", new SessionUser(user));
            return ResponseEntity.ok(Map.of("message", "Update successful"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "User update 실패."));
        }
    }
}
