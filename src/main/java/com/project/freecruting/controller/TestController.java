package com.project.freecruting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.Map;

@Controller
public class TestController {
    @GetMapping("hello")
    public  String hello(Model model) {
        model.addAttribute("data", "hello!!");
        return "hello";

    }

    // 오류 방지용
    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    public ResponseEntity<Map<String, String>> devtoolsJson() {
        // 빈 JSON 응답
        return ResponseEntity.ok(Collections.emptyMap());
    }
}