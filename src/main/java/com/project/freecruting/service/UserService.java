package com.project.freecruting.service;

import com.project.freecruting.dto.user.UserSaveRequestDto;
import com.project.freecruting.dto.user.UserUpdateRequestDto;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.type.Role;
import com.project.freecruting.model.User;
import com.project.freecruting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    public Long save(UserSaveRequestDto dto) {

        if(userRepository.findByEmail(dto.getEmail()) != null) {
            throw new InvalidStateException("이미 존재하는 Email 입니다.");
        }

        return userRepository.save(User.builder()
                .email(dto.getEmail())
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .role(Role.USER)
                .provider("local")
                .picture("empty") // picture 는 빈 상태로 둠
                .build()).getId();
    }

    @Transactional
    public User update(UserUpdateRequestDto requestDto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("해당 유저 없음"));

        user.update(requestDto.getName(), requestDto.getPicture());
        return user;
    }

    @Override
    public User loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }


}
