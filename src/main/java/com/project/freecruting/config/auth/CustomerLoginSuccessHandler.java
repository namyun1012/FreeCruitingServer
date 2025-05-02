package com.project.freecruting.config.auth;

import com.project.freecruting.config.auth.dto.SessionUser;
import com.project.freecruting.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomerLoginSuccessHandler  implements AuthenticationSuccessHandler {
    private final HttpSession httpSession;

    @Autowired
    public CustomerLoginSuccessHandler(HttpSession httpSession) {
        this.httpSession =httpSession;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        User user = ((User) authentication.getPrincipal()); // User 엔티티 추출
        SessionUser sessionUser = new SessionUser(user);

        httpSession.setAttribute("user", sessionUser);

        response.sendRedirect("/");
    }

}
