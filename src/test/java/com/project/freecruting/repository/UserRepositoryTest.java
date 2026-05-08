package com.project.freecruting.repository;

import com.project.freecruting.model.User;
import com.project.freecruting.model.type.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .name("테스터")
                .email(email)
                .role(Role.USER)
                .build());
    }

    // ──────────────────────────────────────────
    // findByEmail()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("존재하는 이메일로 조회 시 해당 사용자를 반환한다")
        void findByEmail_existingEmail_returnsUser() {
            saveUser("test@example.com");

            Optional<User> result = userRepository.findByEmail("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional을 반환한다")
        void findByEmail_nonExistentEmail_returnsEmpty() {
            Optional<User> result = userRepository.findByEmail("notexist@example.com");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이메일이 정확히 일치해야 한다 — 부분 문자열로는 조회되지 않는다")
        void findByEmail_partialEmail_returnsEmpty() {
            saveUser("test@example.com");

            Optional<User> result = userRepository.findByEmail("test");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이메일이 유사한 다른 사용자와 혼동되지 않는다")
        void findByEmail_similarEmails_returnsExactMatch() {
            saveUser("a@example.com");
            userRepository.save(User.builder()
                    .name("다른사용자")
                    .email("ab@example.com")
                    .role(Role.USER)
                    .build());

            Optional<User> result = userRepository.findByEmail("a@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("a@example.com");
        }

        @Test
        @DisplayName("OAuth2 사용자도 이메일로 조회된다")
        void findByEmail_oauthUser_returnsUser() {
            userRepository.save(User.builder()
                    .name("구글사용자")
                    .email("google@gmail.com")
                    .role(Role.USER)
                    .provider("google")
                    .build());

            Optional<User> result = userRepository.findByEmail("google@gmail.com");

            assertThat(result).isPresent();
            assertThat(result.get().getProvider()).isEqualTo("google");
        }
    }
}
