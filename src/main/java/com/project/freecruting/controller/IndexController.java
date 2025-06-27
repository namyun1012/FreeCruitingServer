package com.project.freecruting.controller;

import com.project.freecruting.config.auth.LoginUser;
import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.dto.comment.CommentListResponseDto;
import com.project.freecruting.dto.party.PartyJoinRequestListResponseDto;
import com.project.freecruting.dto.party.PartyListResponseDto;
import com.project.freecruting.dto.party.PartyMemberListResponseDto;
import com.project.freecruting.dto.party.PartyResponseDto;
import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.model.type.SearchType;
import com.project.freecruting.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// 추후 react.js 사용한 것으로 바꿀 것
// 여기있는 것들은 react에서 못쓸 듯, 임시용도
@RequiredArgsConstructor
@Controller
public class IndexController {
    private final PostService postService;
    private final CommentService commentService;
    private final PartyService partyService;
    private final PartyMemberService partyMemberService;
    private final PartyJoinRequestService partyJoinRequestService;

    private static final String PAGE_DEFAULT_VALUE = "1";
    private static final String SIZE_DEFAULT_VALUE = "15";

    // 전체 용도
    @GetMapping("/")
    public String index(Model model, @LoginUser SessionUser user,
                        @RequestParam(defaultValue = PAGE_DEFAULT_VALUE) int page, @RequestParam(defaultValue = SIZE_DEFAULT_VALUE) int size) {
        Page<PostListResponseDto> postPage = postService.findAllPage(page - 1, size);

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
                            @RequestParam(defaultValue = PAGE_DEFAULT_VALUE) int page, @RequestParam(defaultValue = SIZE_DEFAULT_VALUE) int size) {

        Page<PostListResponseDto> postPage;
        postPage = postService.findByType(type, page - 1, size);

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
    public String postUpdate(@PathVariable Long id, Model model, @LoginUser SessionUser user)
    {
        PostResponseDto dto = postService.findById(id, user.getId());
        model.addAttribute("post", dto);
        return "post-update";
    }

    @GetMapping("/post/read/{id}")
    public String postRead(@PathVariable Long id, Model model, @LoginUser SessionUser user,
                           @RequestParam(defaultValue = PAGE_DEFAULT_VALUE) int page, @RequestParam(defaultValue = SIZE_DEFAULT_VALUE) int size) {

        PostResponseDto dto;
        if (user == null)  dto = postService.findById(id, null);
        else dto = postService.findById(id, user.getId());

        model.addAttribute("post",dto);

        // 댓글 Paging 넣어 주기
        Page<CommentListResponseDto> commentPage = commentService.findAllPageByPostId(page - 1, size, id);

        model = supportPaging(model, commentPage);
        model.addAttribute("comments", commentPage.getContent());


        return "post-read";
    }
    
    // 검색 기능, front 에서 query 와 search_type 받아오기
    @GetMapping("/post/search/{type}/{query}")
    public String searchResult(Model model, @LoginUser SessionUser user, @PathVariable String query, @PathVariable String type,
                               @RequestParam(defaultValue = PAGE_DEFAULT_VALUE) int page,
                               @RequestParam(defaultValue = SIZE_DEFAULT_VALUE) int size) {
        if(query.isBlank()) {
            return "redirect:/";
        }

        SearchType searchType = SearchType.fromString(type);
        Page<PostListResponseDto> postPage = postService.search(query, searchType, page - 1, size);
        model.addAttribute("posts", postPage.getContent());
        model = supportPaging(model, postPage);

        model.addAttribute("query", query);
        model.addAttribute("type", type);

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "post-search";
    }

    @GetMapping("/user")
    public String userRead(Model model, @LoginUser SessionUser user,
                           @RequestParam(defaultValue = PAGE_DEFAULT_VALUE) int page,
                           @RequestParam(defaultValue = SIZE_DEFAULT_VALUE) int size) {
        if(user == null) {
            return "/";
        }

        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userPicture", user.getPicture());

        Page<PartyMemberListResponseDto> partyMemberPage = partyMemberService.findByUserId(user.getId(),page - 1, size);
        model.addAttribute("partyMembers", partyMemberPage);
        model = supportPaging(model, partyMemberPage);

        return "user-read";
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

    @GetMapping("/party")
    public String partys(Model model, @LoginUser SessionUser user,
                         @RequestParam(defaultValue = PAGE_DEFAULT_VALUE) int page, @RequestParam(defaultValue = SIZE_DEFAULT_VALUE) int size) {
        Page<PartyListResponseDto> partyPage = partyService.findAllPage(page - 1, size);

        model.addAttribute("partys", partyPage.getContent());
        model = supportPaging(model, partyPage);

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "party-list";
    }

    @GetMapping("/party/save")
    public String partySave(Model model, @LoginUser SessionUser user) {

        if(user != null) {
            model.addAttribute("userName", user.getName());
        }

        return "party-save";
    }

    @GetMapping("/party/update/{id}")
    public String partyUpdate(@PathVariable Long id, Model model)
    {
        PartyResponseDto party = partyService.findById(id);
        model.addAttribute("party", party);

        // Party Member 들을 보여줌.
        List<PartyMemberListResponseDto> partyMembers = partyMemberService.findByPartyId(party.getId());

        List<PartyJoinRequestListResponseDto> partyJoinRequests = partyJoinRequestService.findPendingByPartyId(party.getId());
        model.addAttribute("partyMembers", partyMembers);
        model.addAttribute("partyJoinRequests", partyJoinRequests);
        return "party-update";
    }

    @GetMapping("/party/read/{id}")
    public String partyRead(@PathVariable Long id, Model model, @LoginUser SessionUser user) {
        PartyResponseDto party = partyService.findById(id);
        model.addAttribute("party", party);

        List<PartyMemberListResponseDto> partyMembers = partyMemberService.findByPartyId(party.getId());
        model.addAttribute("partyMembers", partyMembers);
        
        return "party-read";
    }

    // Paging 사용시 Page Support 하기 위함
    private Model supportPaging(Model model, Page<?> page) {

        int currentPage = page.getNumber();
        int totalPages = page.getTotalPages();

        int maxPagesToShow = 10;
        int startPage, endPage;

        if (totalPages <= maxPagesToShow) {
            startPage = 0;
            endPage = totalPages - 1;
        } else {
            startPage = Math.max(0, currentPage - (maxPagesToShow / 2));
            endPage = Math.min(totalPages - 1, currentPage + (maxPagesToShow / 2) - (maxPagesToShow % 2 == 0 ? 1 : 0));

            if (endPage - startPage + 1 < maxPagesToShow) {
                if(startPage == 0) {
                    endPage = Math.min(totalPages - 1, startPage + maxPagesToShow - 1);
                }

                else if(endPage == totalPages - 1) {
                    startPage = Math.max(0, totalPages - maxPagesToShow);
                }
            }
        }

        List<PageInfo> pages = new ArrayList<>();

        for (int i = startPage; i <= endPage; i++) {
            pages.add(new PageInfo(i + 1, i == currentPage));
        }

        model.addAttribute("pages", pages);
        model.addAttribute("hasPrevPage", page.hasPrevious());
        model.addAttribute("prevPage", page.previousOrFirstPageable().getPageNumber() + 1); // 1부터 시작하도록 +1
        model.addAttribute("hasNextPage", page.hasNext());
        model.addAttribute("nextPage", page.nextOrLastPageable().getPageNumber() + 1); // 1부터 시작하도록 +1
        model.addAttribute("currentPage", currentPage + 1); // 현재 페이지 번호도 1부터 시작하도록 +1

        return model;
    }

    public static class PageInfo {
        public int number;
        public boolean isCurrent;

        public PageInfo(int number, boolean isCurrent) {
            this.number = number;
            this.isCurrent = isCurrent;
        }
    }
}
