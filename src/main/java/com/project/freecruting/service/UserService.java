package com.project.freecruting.service;

import com.project.freecruting.dto.user.UserUpdateRequestDto;
import com.project.freecruting.model.User;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User update(UserUpdateRequestDto requestDto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("해당 유저 없음"));

        user.update(requestDto.getName(), requestDto.getPicture());
        return user;
    }


}
