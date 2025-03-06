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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// 추후 react.js 사용한 것으로 바꿀 것
// 여기있는 것들은 react에서 못쓸 듯, 임시용도
@RequiredArgsConstructor
@Controller
public class IndexController {
    private final PostService postService;
    private final CommentService commentService;
    private final HttpSession httpSession;
    
    // 전체 용도
    @GetMapping("/")
    public String index(Model model, @LoginUser SessionUser user,
                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        Page<PostListResponseDto> postPage = postService.findAllPage(page, size);

        model.addAttribute("posts", postPage.getContent());
        model = supportPaging(model, postPage);

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "index";
    }
    
    // type 변환 용도
    @GetMapping("/posts/{type}")
    public String postsType(Model model, @LoginUser SessionUser user, @PathVariable String type,
                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {

        Page<PostListResponseDto> postPage;
        postPage = postService.findByType(type, page, size);

        model.addAttribute("posts", postPage.getContent());
        model = supportPaging(model, postPage);

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

        model = supportPaging(model, commentPage);
        model.addAttribute("comments", commentPage.getContent());


        return "post-read";
    }
    
    // 검색 기능, front 에서 query 와 search_type 받아오기
    @GetMapping("/post/search/{type}/{query}")
    public String searchResult(Model model, @LoginUser SessionUser user, @PathVariable String query, @PathVariable String type,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size) {
        if(query.isBlank()) {
            return "redirect:/";
        }

        SearchType searchType = SearchType.fromString(type);
        Page<PostListResponseDto> postPage = postService.search(query, searchType, page, size);
        model.addAttribute("posts", postPage.getContent());
        model = supportPaging(model, postPage);

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

    // Paging 사용시 Page Support 하기 위함
    private Model supportPaging(Model model, Page<?> page) {
        model.addAttribute("currentPage", page.getNumber());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("hasPrevPage", page.hasPrevious());
        model.addAttribute("hasNextPage", page.hasNext());
        model.addAttribute("prevPage", Math.max(0, page.getNumber() - 1));
        model.addAttribute("nextPage", page.getNumber() + 1);

        List<Map<String, Object>> pages = IntStream.range(0, page.getTotalPages())
                .mapToObj(i -> {
                    Map<String, Object> pageInfo = new HashMap<>();
                    pageInfo.put("number", i);
                    pageInfo.put("isCurrent", i == page.getNumber());
                    return pageInfo;
                })
                .collect(Collectors.toList());

        model.addAttribute("pages", pages);
        return model;
    }
}
