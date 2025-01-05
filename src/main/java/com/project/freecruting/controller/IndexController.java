package com.project.freecruting.controller;

import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.service.PostService;
import lombok.RequiredArgsConstructor;
import org.h2.engine.Mode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 추후 react.js 사용한 것으로 바꿀 것
// 여기있는 것들은 react에서 못쓸 듯, 임시용도
@RequiredArgsConstructor
@Controller
public class IndexController {
    private final PostService postService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("posts", postService.findAllDesc());
        return "index";
    }

    @GetMapping("/post/save")
    public String postSave() {
        return "post-save";
    }

    @GetMapping("/post/update/{id}")
    public String postUpdate(@PathVariable Long id, Model model)
    {
        PostResponseDto dto = postService.findById(id);
        model.addAttribute("post", dto);
        return "post-update";
    }
}
