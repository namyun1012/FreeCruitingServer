package com.project.freecruting.log;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ApiLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        logger.info("--> API Request: {} {}", request.getMethod(), request.getRequestURI());
        return true; // true를 반환해야 핸들러로 진행됩니다.
    }

    // REST API에서는 거의 사용되지 않음
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("<-- API Response: {} {} Status {} took {} ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

        if (ex != null) {
            logger.error("API Request failed with exception: {}", ex.getMessage());
        }
    }


}
