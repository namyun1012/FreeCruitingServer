package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.post.PostListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*
    IndexController 에 과하게 몰려서 Login 관련 front 는 이쪽에다 작성

 */


@RequiredArgsConstructor
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }


}
