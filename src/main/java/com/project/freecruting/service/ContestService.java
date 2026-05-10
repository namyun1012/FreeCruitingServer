package com.project.freecruting.service;

import com.project.freecruting.dto.contest.ContestListResponseDto;
import com.project.freecruting.dto.contest.ContestResponseDto;
import com.project.freecruting.dto.contest.ContestSaveRequestDto;
import com.project.freecruting.dto.contest.ContestUpdateRequestDto;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Contest;
import com.project.freecruting.model.type.ContestCategory;
import com.project.freecruting.repository.ContestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class ContestService {

    private final ContestRepository contestRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.use-redis-for-views:false}")
    private boolean useRedis;

    private static final String VIEW_COUNT_KEY_PREFIX   = "contest:views:";
    private static final String VIEWED_USERS_KEY_PREFIX  = "contest:viewed_users:";
    private static final int    SCAN_COUNT               = 100;

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Transactional
    public Long save(ContestSaveRequestDto requestDto) {
        return contestRepository.save(requestDto.toEntity()).getId();
    }

    @Transactional
    public Long update(Long id, ContestUpdateRequestDto requestDto) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 공모전 없음. id=" + id));

        contest.update(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getOrganizer(),
                requestDto.parsedCategory(),
                requestDto.getApplicationStartDate(),
                requestDto.getApplicationDeadline(),
                requestDto.getContestStartDate(),
                requestDto.getContestEndDate(),
                requestDto.getImageUrl(),
                requestDto.getOfficialUrl(),
                requestDto.getTarget(),
                requestDto.getRegion()
        );
        return id;
    }

    @Transactional
    public Long delete(Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 공모전 없음. id=" + id));
        contestRepository.delete(contest);
        return id;
    }

    // ── 조회 ──────────────────────────────────────────────────────────────

    @Transactional
    public ContestResponseDto findById(Long id, Long userId) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 공모전 없음. id=" + id));

        if (useRedis && userId != null) {
            try {
                String userSetKey = VIEWED_USERS_KEY_PREFIX + id;
                Long added = redisTemplate.opsForSet().add(userSetKey, userId.toString());

                if (added != null && added > 0) {
                    redisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + id);
                    redisTemplate.expire(userSetKey, Duration.ofHours(24));
                }
            } catch (Exception e) {
                System.err.println("Redis view count error: " + e.getMessage());
            }
        } else {
            contestRepository.increaseViews(id);
        }

        return new ContestResponseDto(contest);
    }

    // 목록 조회 — category 필터 + keyword 검색 지원
    @Transactional(readOnly = true)
    public Page<ContestListResponseDto> findContestPages(String category, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (keyword != null && !keyword.isBlank()) {
            return contestRepository.searchByKeyword(keyword, pageable)
                    .map(ContestListResponseDto::new);
        }

        if (category != null && !category.isBlank()) {
            ContestCategory contestCategory = ContestCategory.valueOf(category.toUpperCase());
            return contestRepository.findByCategoryOrderByIdDesc(contestCategory, pageable)
                    .map(ContestListResponseDto::new);
        }

        return contestRepository.findAllByOrderByIdDesc(pageable)
                .map(ContestListResponseDto::new);
    }

    // ── Redis → DB 동기화 (5분마다) ────────────────────────────────────────

    @Scheduled(fixedRateString = "${app.view-counting.sync-interval-ms:300000}")
    @Transactional
    public void syncViewsFromRedisToDb() {
        if (!useRedis) return;

        ScanOptions options = ScanOptions.scanOptions()
                .match(VIEW_COUNT_KEY_PREFIX + "*")
                .count(SCAN_COUNT)
                .build();

        System.out.println("Contest view sync started.");
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(key -> {
                try {
                    // getAndDelete: 읽기 + 삭제를 원자적으로 처리 → 레이스 컨디션 방지
                    String countStr = redisTemplate.opsForValue().getAndDelete(key);
                    if (countStr == null) return;

                    long increment = Long.parseLong(countStr);
                    long contestId = Long.parseLong(key.substring(VIEW_COUNT_KEY_PREFIX.length()));

                    if (increment > 0) {
                        contestRepository.increaseViews(contestId, increment);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Contest view sync parse error: " + key);
                } catch (Exception e) {
                    System.err.println("Contest view sync error for " + key + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Contest view sync scan error: " + e.getMessage());
        }
        System.out.println("Contest view sync finished.");
    }
}
