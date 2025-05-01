package com.project.freecruting.config.auth;

import com.project.freecruting.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@RequiredArgsConstructor
@Configuration
public class SecurityConfig {
    private final CustomerOAuth2UserService customerOAuth2UserService;

    // 현재 로그 아웃 상태면 js 를 못 불러오는 듯?
    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()
                .requestMatchers("/index.js", "/js/**", "/css/**", "/images/**", "/webjars/**", "/static/**");
    }
    
    // 현재 로그인 안하면 js 못 불러오는 중
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf.disable()) // 임시로 비활성화, 켜 놓는 것이 좋긴 함
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/h2-console/**", "/profile").permitAll()
                        .requestMatchers("/login", "/signup").permitAll()
                        .requestMatchers("/api/v1/post/**", "/post/read/**").permitAll()
                        .requestMatchers("/api/v1/user/**").permitAll()
                        .requestMatchers("/api/v1/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                )
                // Oauth2Login Form
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .permitAll()
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customerOAuth2UserService)
                        )
                        .successHandler(((request, response, authentication) -> {
                            response.sendRedirect("/");
                        }))
                )
                .formLogin(form -> form
                        .loginPage("/login")              // 로그인 폼 페이지
                        .defaultSuccessUrl("/")
                        .permitAll()
                )

                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.disable()) // HSTS 비활성화
                        .addHeaderWriter((request, response) -> {
                            // X-Frame-Options 헤더를 직접 설정
                            response.setHeader("X-Frame-Options", "ALLOWALL");
                        })
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder,
                                                       UserService userService) throws Exception {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
