package com.project.freecruting.model.type;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    POST_COMMENT("게시글에 댓글이 달렸습니다"),
    PARTY_JOIN_REQUEST("파티에 가입 신청이 들어왔습니다."),
    DEFAULT_NOTIFICATION("알람이 생성 되었습니다."),

    // [미구현]
    COMMENT_REPLY("댓글에 답글이 달렸습니다");

    private final String description;
}
