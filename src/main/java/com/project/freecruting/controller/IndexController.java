package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.comment.CommentListResponseDto;
import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.SearchType;
import com.project.freecruting.service.CommentService;
import com.project.freecruting.service.PostService;
import com.project.freecruting.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// 추후 react.js 사용한 것으로 바꿀 것
// 여기있는 것들은 react에서 못쓸 듯, 임시용도
@RequiredArgsConstructor
@Controller
public class IndexController {
    private final PostService postService;
    private final CommentService commentService;
    private final HttpSession httpSession;
    @GetMapping("/")
    public String index(Model model, @LoginUser SessionUser user,
                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        Page<PostListResponseDto> postPage = postService.findAllPage(page, size);

        model.addAttribute("posts", postPage.getContent());
        
        // 페이지 위한 용도
        model.addAttribute("currentPage", postPage.getNumber());
        model.addAttribute("totalPages", postPage.getTotalPages());
        model.addAttribute("hasPrevPage", postPage.hasPrevious());
        model.addAttribute("hasNextPage", postPage.hasNext());
        model.addAttribute("prevPage", Math.max(0, postPage.getNumber() - 1));
        model.addAttribute("nextPage", postPage.getNumber() + 1);

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
    public String postRead(@PathVariable Long id, Model model, @LoginUser SessionUser user,
                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        PostResponseDto dto = postService.findById(id);
        model.addAttribute("post",dto);

        // 댓글 Paging 넣어 주기
        Page<CommentListResponseDto> commentPage = commentService.findAllPageByPostId(page, size, id);

        model.addAttribute("comments", commentPage.getContent());

        model.addAttribute("currentPage", commentPage.getNumber());
        model.addAttribute("totalPages", commentPage.getTotalPages());
        model.addAttribute("hasPrevPage", commentPage.hasPrevious());
        model.addAttribute("hasNextPage", commentPage.hasNext());
        model.addAttribute("prevPage", Math.max(0, commentPage.getNumber() - 1));
        model.addAttribute("nextPage", commentPage.getNumber() + 1);

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
