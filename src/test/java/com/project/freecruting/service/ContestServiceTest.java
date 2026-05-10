package com.project.freecruting.service;

import com.project.freecruting.dto.contest.ContestListResponseDto;
import com.project.freecruting.dto.contest.ContestResponseDto;
import com.project.freecruting.dto.contest.ContestSaveRequestDto;
import com.project.freecruting.dto.contest.ContestUpdateRequestDto;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Contest;
import com.project.freecruting.model.type.ContestCategory;
import com.project.freecruting.repository.ContestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @InjectMocks
    private ContestService contestService;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    // id는 JPA가 채워주는 필드라 ReflectionTestUtils로 세팅
    private Contest createContest(Long id) {
        Contest contest = Contest.builder()
                .title("테스트 공모전")
                .organizer("주최기관")
                .description("설명")
                .category(ContestCategory.PROGRAMMING)
                .applicationStartDate(LocalDate.now().minusDays(1))
                .applicationDeadline(LocalDate.now().plusDays(30))
                .build();
        ReflectionTestUtils.setField(contest, "id", id);
        return contest;
    }

    // ──────────────────────────────────────────
    // save()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("저장 성공 시 공모전 ID를 반환한다")
        void save_success_returnsId() {
            ContestSaveRequestDto dto = ContestSaveRequestDto.builder()
                    .title("공모전").organizer("주최").category("PROGRAMMING")
                    .applicationStartDate(LocalDate.now())
                    .applicationDeadline(LocalDate.now().plusDays(30))
                    .build();
            Contest saved = createContest(1L);
            given(contestRepository.save(any(Contest.class))).willReturn(saved);

            Long result = contestService.save(dto);

            assertThat(result).isEqualTo(1L);
            verify(contestRepository).save(any(Contest.class));
        }
    }

    // ──────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("수정 성공 시 변경 내용이 반영되고 ID를 반환한다")
        void update_success_returnsId() {
            Long contestId = 1L;
            Contest contest = createContest(contestId);
            given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));

            ContestUpdateRequestDto dto = ContestUpdateRequestDto.builder()
                    .title("수정된 제목").organizer("수정된 주최").category("DESIGN")
                    .applicationStartDate(LocalDate.now())
                    .applicationDeadline(LocalDate.now().plusDays(60))
                    .build();

            Long result = contestService.update(contestId, dto);

            assertThat(result).isEqualTo(contestId);
            assertThat(contest.getTitle()).isEqualTo("수정된 제목");
            assertThat(contest.getOrganizer()).isEqualTo("수정된 주최");
            assertThat(contest.getCategory()).isEqualTo(ContestCategory.DESIGN);
        }

        @Test
        @DisplayName("존재하지 않는 공모전이면 NotFoundException 발생")
        void update_notFound_throwsNotFoundException() {
            given(contestRepository.findById(anyLong())).willReturn(Optional.empty());

            ContestUpdateRequestDto dto = ContestUpdateRequestDto.builder()
                    .title("제목").organizer("주최").category("PROGRAMMING")
                    .applicationStartDate(LocalDate.now())
                    .applicationDeadline(LocalDate.now().plusDays(30))
                    .build();

            assertThatThrownBy(() -> contestService.update(999L, dto))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // delete()
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("삭제 성공 시 delete()가 호출되고 ID를 반환한다")
        void delete_success_returnsId() {
            Long contestId = 1L;
            Contest contest = createContest(contestId);
            given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));

            Long result = contestService.delete(contestId);

            assertThat(result).isEqualTo(contestId);
            verify(contestRepository).delete(contest);
        }

        @Test
        @DisplayName("존재하지 않는 공모전이면 NotFoundException 발생")
        void delete_notFound_throwsNotFoundException() {
            given(contestRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> contestService.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // findById() — 조회수 처리 분기
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Redis 미사용 시 DB 조회수 증가(increaseViews)를 호출한다")
        void findById_redisDisabled_incrementsDb() {
            ReflectionTestUtils.setField(contestService, "useRedis", false);
            Long contestId = 1L;
            Contest contest = createContest(contestId);
            given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));

            ContestResponseDto result = contestService.findById(contestId, 1L);

            verify(contestRepository).increaseViews(contestId);
            assertThat(result.getId()).isEqualTo(contestId);
        }

        @Test
        @DisplayName("Redis 활성화여도 비로그인(userId=null)이면 DB 조회수 증가를 호출한다")
        void findById_redisEnabled_nullUser_incrementsDb() {
            ReflectionTestUtils.setField(contestService, "useRedis", true);
            Long contestId = 1L;
            Contest contest = createContest(contestId);
            given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));

            contestService.findById(contestId, null);

            verify(contestRepository).increaseViews(contestId);
            verify(redisTemplate, never()).opsForSet();
        }

        @Test
        @DisplayName("Redis 사용, 첫 조회이면 Redis 조회수를 증가시키고 DB는 건드리지 않는다")
        void findById_redisEnabled_firstView_incrementsRedis() {
            ReflectionTestUtils.setField(contestService, "useRedis", true);
            Long contestId = 1L, userId = 5L;
            Contest contest = createContest(contestId);
            given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add(anyString(), anyString())).willReturn(1L); // 신규 사용자
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            contestService.findById(contestId, userId);

            verify(valueOperations).increment(anyString());
            verify(contestRepository, never()).increaseViews(contestId);
        }

        @Test
        @DisplayName("Redis 사용, 24시간 내 재조회이면 조회수를 올리지 않는다")
        void findById_redisEnabled_duplicateView_skipsIncrement() {
            ReflectionTestUtils.setField(contestService, "useRedis", true);
            Long contestId = 1L, userId = 5L;
            Contest contest = createContest(contestId);
            given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.add(anyString(), anyString())).willReturn(0L); // 이미 조회한 사용자

            contestService.findById(contestId, userId);

            verify(redisTemplate, never()).opsForValue();
            verify(contestRepository, never()).increaseViews(contestId);
        }

        @Test
        @DisplayName("존재하지 않는 공모전이면 NotFoundException 발생")
        void findById_notFound_throwsNotFoundException() {
            given(contestRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> contestService.findById(999L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // findContestPages() — 파라미터에 따른 분기 검증
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("findContestPages()")
    class FindContestPages {

        private final Page<Contest> emptyPage = new PageImpl<>(Collections.emptyList());

        @Test
        @DisplayName("keyword가 있으면 searchByKeyword()를 호출한다")
        void findContestPages_withKeyword_callsSearchByKeyword() {
            given(contestRepository.searchByKeyword(anyString(), any(Pageable.class))).willReturn(emptyPage);

            contestService.findContestPages(null, "AI공모전", 0, 10);

            verify(contestRepository).searchByKeyword(eq("AI공모전"), any(Pageable.class));
            verify(contestRepository, never()).findAllByOrderByIdDesc(any());
        }

        @Test
        @DisplayName("keyword와 category가 동시에 있으면 keyword 우선으로 searchByKeyword()를 호출한다")
        void findContestPages_keywordTakesPriorityOverCategory() {
            given(contestRepository.searchByKeyword(anyString(), any(Pageable.class))).willReturn(emptyPage);

            contestService.findContestPages("PROGRAMMING", "AI공모전", 0, 10);

            verify(contestRepository).searchByKeyword(eq("AI공모전"), any(Pageable.class));
            verify(contestRepository, never()).findByCategoryOrderByIdDesc(any(), any());
        }

        @Test
        @DisplayName("category만 있으면 findByCategoryOrderByIdDesc()를 호출한다")
        void findContestPages_withCategoryOnly_callsFindByCategory() {
            given(contestRepository.findByCategoryOrderByIdDesc(
                    eq(ContestCategory.PROGRAMMING), any(Pageable.class))).willReturn(emptyPage);

            contestService.findContestPages("PROGRAMMING", null, 0, 10);

            verify(contestRepository).findByCategoryOrderByIdDesc(
                    eq(ContestCategory.PROGRAMMING), any(Pageable.class));
            verify(contestRepository, never()).findAllByOrderByIdDesc(any());
        }

        @Test
        @DisplayName("파라미터가 없으면 전체 목록 조회를 호출한다")
        void findContestPages_noParams_callsFindAll() {
            given(contestRepository.findAllByOrderByIdDesc(any(Pageable.class))).willReturn(emptyPage);

            Page<ContestListResponseDto> result = contestService.findContestPages(null, null, 0, 10);

            verify(contestRepository).findAllByOrderByIdDesc(any(Pageable.class));
            assertThat(result.getContent()).isEmpty();
        }
    }
}
