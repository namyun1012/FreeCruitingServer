package com.project.freecruting.service;

import com.project.freecruting.dto.user.UserSaveRequestDto;
import com.project.freecruting.exception.InvalidStateException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.Role;
import com.project.freecruting.repository.UserRepository;
import com.project.freecruting.service.infra.storage.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileService fileService;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // id는 JPA가 채워주는 필드라 Builder로 못 세팅 → ReflectionTestUtils 사용
    private User createUser(Long id, String email, String name, String picture) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password("encodedPassword")
                .role(Role.USER)
                .provider("local")
                .picture(picture)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    // ──────────────────────────────────────────
    // save()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("신규 유저 저장 성공 시 User ID를 반환한다")
        void save_success_returnsId() {
            UserSaveRequestDto dto = UserSaveRequestDto.builder()
                    .email("test@example.com")
                    .password("password123")
                    .name("홍길동")
                    .build();
            User savedUser = createUser(1L, "test@example.com", "홍길동", "favicon.ico");

            given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.empty());
            given(bCryptPasswordEncoder.encode(dto.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            Long result = userService.save(dto);

            assertThat(result).isEqualTo(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("이메일을 소문자로 변환하여 저장한다")
        void save_emailConvertedToLowerCase() {
            UserSaveRequestDto dto = UserSaveRequestDto.builder()
                    .email("TEST@EXAMPLE.COM")
                    .password("password123")
                    .name("홍길동")
                    .build();
            User savedUser = createUser(1L, "test@example.com", "홍길동", "favicon.ico");

            given(userRepository.findByEmail("TEST@EXAMPLE.COM")).willReturn(Optional.empty());
            given(bCryptPasswordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            userService.save(dto);

            // 저장 시 소문자 이메일로 User가 생성되었는지 확인
            verify(userRepository).save(argThat(user -> user.getEmail().equals("test@example.com")));
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 InvalidStateException 발생")
        void save_duplicateEmail_throwsInvalidStateException() {
            UserSaveRequestDto dto = UserSaveRequestDto.builder()
                    .email("duplicate@example.com")
                    .password("password123")
                    .name("홍길동")
                    .build();
            User existingUser = createUser(1L, "duplicate@example.com", "기존유저", "favicon.ico");

            given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.save(dto))
                    .isInstanceOf(InvalidStateException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("저장 시 비밀번호가 BCrypt로 인코딩된다")
        void save_passwordIsEncoded() {
            UserSaveRequestDto dto = UserSaveRequestDto.builder()
                    .email("test@example.com")
                    .password("rawPassword")
                    .name("홍길동")
                    .build();
            User savedUser = createUser(1L, "test@example.com", "홍길동", "favicon.ico");

            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(bCryptPasswordEncoder.encode("rawPassword")).willReturn("$2a$encoded");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            userService.save(dto);

            verify(bCryptPasswordEncoder).encode("rawPassword");
            verify(userRepository).save(argThat(user -> user.getPassword().equals("$2a$encoded")));
        }
    }

    // ──────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("파일 없이 이름만 변경하면 기존 프로필 이미지를 유지한다")
        void update_nameOnly_keepsPicture() {
            User user = createUser(1L, "test@example.com", "홍길동", "old-picture.jpg");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

            User result = userService.update("새이름", null, "test@example.com");

            assertThat(result.getName()).isEqualTo("새이름");
            assertThat(result.getPicture()).isEqualTo("old-picture.jpg");
        }

        @Test
        @DisplayName("파일이 있으면 새 이미지로 업데이트하고 기존 파일을 삭제한다")
        void update_withFile_updatesImageAndDeletesOld() throws IOException {
            User user = createUser(1L, "test@example.com", "홍길동", "old-picture.jpg");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "new-picture.jpg", "image/jpeg", "image-data".getBytes());
            given(fileService.uploadFile(file)).willReturn("new-picture.jpg");

            User result = userService.update("홍길동", file, "test@example.com");

            assertThat(result.getPicture()).isEqualTo("/api/v1/files/new-picture.jpg");
            verify(fileService).deleteFile("old-picture.jpg");
        }

        @Test
        @DisplayName("파일 업로드는 성공했지만 이전 파일 삭제가 실패해도 예외를 던지지 않는다")
        void update_deleteOldFileFails_noException() throws IOException {
            User user = createUser(1L, "test@example.com", "홍길동", "old-picture.jpg");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "new-picture.jpg", "image/jpeg", "image-data".getBytes());
            given(fileService.uploadFile(file)).willReturn("new-picture.jpg");
            doThrow(new IOException("삭제 실패")).when(fileService).deleteFile("old-picture.jpg");

            // 예외 없이 정상 완료되어야 한다
            User result = userService.update("홍길동", file, "test@example.com");

            assertThat(result.getPicture()).isEqualTo("/api/v1/files/new-picture.jpg");
        }

        @Test
        @DisplayName("파일 업로드 중 IOException 발생 시 기존 이미지를 유지한다")
        void update_fileUploadFails_keepOldPicture() throws IOException {
            User user = createUser(1L, "test@example.com", "홍길동", "old-picture.jpg");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "new-picture.jpg", "image/jpeg", "image-data".getBytes());
            given(fileService.uploadFile(file)).willThrow(new IOException("업로드 실패"));

            User result = userService.update("홍길동", file, "test@example.com");

            assertThat(result.getPicture()).isEqualTo("old-picture.jpg");
            verify(fileService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 NotFoundException 발생")
        void update_userNotFound_throwsNotFoundException() {
            given(userRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update("이름", null, "notfound@example.com"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // loadUserByUsername()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsername {

        @Test
        @DisplayName("이메일로 유저 조회 성공 시 User 엔티티를 반환한다")
        void loadUserByUsername_found_returnsUser() {
            User user = createUser(1L, "test@example.com", "홍길동", "favicon.ico");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

            User result = (User) userService.loadUserByUsername("test@example.com");

            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 UsernameNotFoundException 발생")
        void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
            given(userRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.loadUserByUsername("notfound@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }
}
