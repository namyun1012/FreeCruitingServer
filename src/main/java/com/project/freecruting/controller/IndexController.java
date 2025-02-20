package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.model.SearchType;
import com.project.freecruting.service.PostService;
import com.project.freecruting.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// 추후 react.js 사용한 것으로 바꿀 것
// 여기있는 것들은 react에서 못쓸 듯, 임시용도
@RequiredArgsConstructor
@Controller
public class IndexController {
    private final PostService postService;
    private final HttpSession httpSession;
    @GetMapping("/")
    public String index(Model model, @LoginUser SessionUser user) {
        model.addAttribute("posts", postService.findAllDesc());

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "index";
    }

    @GetMapping("/post/save")
    public String postSave(Model model, @LoginUser SessionUser user) {

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "post-save";
    }

    @GetMapping("/post/update/{id}")
    public String postUpdate(@PathVariable Long id, Model model)
    {
        PostResponseDto dto = postService.findById(id);
        model.addAttribute("post", dto);
        return "post-update";
    }

    @GetMapping("/post/read/{id}")
    public String postRead(@PathVariable Long id, Model model) {
        PostResponseDto dto = postService.findById(id);
        model.addAttribute("post",dto);
        return "post-read";
    }
    
    // 검색 기능, front 에서 query 와 search_type 받아오기
    @PostMapping("/post/search")
    public String searchResult(Model model, @LoginUser SessionUser user, @RequestParam String query, @RequestParam String type) {
        if(query.isBlank()) {
            return "redirect:/";
        }

        SearchType searchType = SearchType.fromString(type);

        model.addAttribute("posts", postService.search(query, searchType));
        model.addAttribute("query", query);
        model.addAttribute("type", type);

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "post-search";
    }

    @GetMapping("/user/update")
    public String userUpdate(Model model, @LoginUser SessionUser user)
    {
        if(user == null) {
            return "/";
        }

        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userPicture", user.getPicture());

        return "user-update";
    }
}
