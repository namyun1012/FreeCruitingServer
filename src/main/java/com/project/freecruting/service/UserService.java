package com.project.freecruting.service;

import com.project.freecruting.dto.user.UserSaveRequestDto;
import com.project.freecruting.dto.user.UserUpdateRequestDto;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.type.Role;
import com.project.freecruting.model.User;
import com.project.freecruting.repository.UserRepository;
import com.project.freecruting.service.storage.FileService;
import com.project.freecruting.service.storage.LocalFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final FileService fileService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    @Transactional
    public Long save(UserSaveRequestDto dto) {

        if(userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new InvalidStateException("이미 존재하는 Email 입니다.");
        }

        return userRepository.save(User.builder()
                .email(dto.getEmail().toLowerCase())
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .role(Role.USER)
                .provider("local")
                .picture("favicon.ico") // picture 는 빈 상태로 둠
                .build()).getId();
    }

    @Transactional
    public User update(String name, MultipartFile file, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("해당 유저 없음"));

        String pictureUrl = null;
        String oldPictureFileName = user.getPicture();

        // User Profile Image 는 내부 저장소 뿐만 아닌 외부 저장소도 가능함.
        if (file != null && !file.isEmpty()) {
            try {
                pictureUrl = "/api/v1/files/" + fileService.uploadFile(file);
            }
            catch (IOException e) {
                System.out.println("Failed in File Upload");
            }
        }

        UserUpdateRequestDto requestDto = UserUpdateRequestDto.builder()
                .name(name)
                .picture(pictureUrl)
                .build();

        if (requestDto.getPicture() != null && !requestDto.getPicture().isEmpty()) {
            user.update(requestDto.getName(), requestDto.getPicture());


            if (oldPictureFileName != null && !oldPictureFileName.isEmpty()) {
                try {
                    fileService.deleteFile(oldPictureFileName);
                    System.out.println("기존 이미지 파일 삭제 성공");
                }
                catch (IOException e) {
                    System.err.println("기존 이미지 파일 삭제 실패");
                }
            }

        }

        else {
            user.update(requestDto.getName(), oldPictureFileName);
        }



        return user;
    }

    @Override
    public User loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }


}
