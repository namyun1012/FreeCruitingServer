package com.project.freecruting.service;

import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.dto.user.UserUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.Users;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public Users update(UserUpdateRequestDto requestDto, String email) {
        Users user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("해당 유저 없음"));

        user.update(requestDto.getName(), requestDto.getPicture());
        return user;
    }


}
