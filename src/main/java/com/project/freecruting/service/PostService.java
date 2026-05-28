package com.project.freecruting.service;

import com.project.freecruting.dto.post.PostListResponseDto;
import com.project.freecruting.dto.post.PostResponseDto;
import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.exception.ForbiddenException;
import com.project.freecruting.exception.NotFoundException;
import com.project.freecruting.model.Post;
import com.project.freecruting.model.User;
import com.project.freecruting.model.type.SearchType;
import com.project.freecruting.repository.CommentRepository;
import com.project.freecruting.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentService commentService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.use-redis-for-views:false}")
    private boolean useRedis;

    private static final String VIEW_COUNT_KEY_PREFIX = "post:views:";
    // daily bucket 키: post:viewed:{postId}:{yyyyMMdd}
    // 하루 단위로 분리되어 당일 UV 수준으로 Set 크기가 자연 제한됨
    private static final String VIEWED_KEY_PREFIX = "post:viewed:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    // Redis SCAN 커서 이동당 반환 힌트 값. Redis가 정확히 보장하지는 않으며,
    // 조회 대상 게시글이 많아질 경우 튜닝할 수 있다.
    private static final int REDIS_SCAN_COUNT = 100;

    @Transactional
    public Long save(PostSaveRequestDto requestDto, Long author_id) {
        return postRepository.save(requestDto.toEntity(author_id)).getId();
    }

    @Transactional
    public Long update(Long id, PostUpdateRequestDto requestDto, Long author_id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 게시글 없음. id=" + id));
        Long post_author_id = post.getAuthor_id();

        if (!post_author_id.equals(author_id)) {
            throw new ForbiddenException("해당 post의 작성자가 아닙니다");
        }

        post.update(requestDto.getTitle(), requestDto.getContent(), requestDto.getImageURL(), Post.PostType.valueOf(requestDto.getType()));
        return id;
    }

    @Transactional
    public Long delete(Long id, Long author_id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 게시글 없음. id = " + id));

        Long post_author_id = post.getAuthor_id();

        if (!post_author_id.equals(author_id)) {
            throw new ForbiddenException("해당 post의 작성자가 아닙니다");
        }

        postRepository.delete(post);
        return id;
    }

    @Transactional
    public PostResponseDto findById(Long id, String identifier) {
        Post entity = postRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 게시글 없음. id=" + id));

        if (useRedis) {
            try {
                // daily bucket: 하루 단위로 키를 분리해 Set 크기를 당일 UV 수준으로 제한
                String today = LocalDate.now().format(DATE_FORMATTER);
                String viewedKey = VIEWED_KEY_PREFIX + id + ":" + today;

                Long addedCount = redisTemplate.opsForSet().add(viewedKey, identifier);

                if (addedCount != null && addedCount > 0) {
                    redisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + id);
                    // 익일 자정으로 만료 시각 고정: 절대 시각이므로 여러 번 호출해도 멱등
                    redisTemplate.expireAt(viewedKey, getNextMidnight());
                }
            } catch (Exception e) {
                // Redis 장애 시 DB 직접 증가로 fallback
                postRepository.increaseViews(id);
            }
            return new PostResponseDto(entity, getRedisViewDelta(id));

        } else {
            // Redis 미사용: DB 직접 증가 (dedup 없음)
            postRepository.increaseViews(id);
            return new PostResponseDto(entity);
        }
    }

    public PostResponseDto findByIdForUpdate(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("해당 게시글 없음"));

        if(!post.getAuthor_id().equals(userId)) {
            throw new ForbiddenException("작성자만 수정 가능");
        }

        return new PostResponseDto(post);

    }
    
    // post pages 반환 함수
    // 일종의 Facade
    public Page<PostListResponseDto> findPostPages(String type, String query, String search_type, int page, int size) {
        Page<PostListResponseDto> postPages;

        if (query != null && search_type != null)  {
            SearchType searchType = SearchType.fromString(search_type);
            postPages = search(query, searchType, page, size);
        }
        else if (type != null) {
            postPages = findByType(type, page, size);
        }
        else {
            postPages = findAllPage(page, size);
        }

        return postPages;
    }


    @Transactional(readOnly = true)
    public List<PostListResponseDto> findAllDesc() {
        return postRepository.findAllDesc().stream()
                .map(PostListResponseDto:: new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> findAllPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = postRepository.findAllByOrderByIdDesc(pageable);

        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        Map<Long, Long> commentCounts = getCommentCountsByPostIds(postIds);
        Map<Long, Long> redisDeltas = getRedisViewDeltas(postIds);

        return posts.map(post -> new PostListResponseDto(
                post,
                commentCounts.get(post.getId()),
                redisDeltas.getOrDefault(post.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> findByType(String type, int page, int size) {
        Post.PostType postType = Post.PostType.fromString(type);
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = postRepository.findByTypeOrderByIdDesc(pageable, postType);

        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        Map<Long, Long> commentCounts = getCommentCountsByPostIds(postIds);
        Map<Long, Long> redisDeltas = getRedisViewDeltas(postIds);

        return posts.map(post -> new PostListResponseDto(
                post,
                commentCounts.get(post.getId()),
                redisDeltas.getOrDefault(post.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> search(String query, SearchType searchType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts;

        if(searchType == SearchType.ALL) {
            posts = postRepository.searchByKeyword(query, pageable);
        } else if(searchType == SearchType.TITLE) {
            posts = postRepository.findByTitle(query, pageable);
        } else if(searchType == SearchType.CONTENT) {
            posts = postRepository.findByContent(query, pageable);
        } else if(searchType == SearchType.AUTHOR) {
            posts = postRepository.findByAuthor(query, pageable);
        } else {
            posts = postRepository.searchByKeyword(query, pageable);
        }

        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        Map<Long, Long> commentCounts = getCommentCountsByPostIds(postIds);
        Map<Long, Long> redisDeltas = getRedisViewDeltas(postIds);

        return posts.map(post -> new PostListResponseDto(
                post,
                commentCounts.get(post.getId()),
                redisDeltas.getOrDefault(post.getId(), 0L)));
    }

    // Support Function
    // post view redis schedule
    @Scheduled(fixedRateString = "${app.view-counting.sync-interval-ms:300000}")
    @Transactional
    public void syncViewsFromRedisToDb() {
        if (!useRedis) return;

        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(VIEW_COUNT_KEY_PREFIX + "*")
                .count(REDIS_SCAN_COUNT)
                .build();

        System.out.println("Redis to DB view sync started.");

        // SCAN: keys() 와 달리 커서 기반으로 반복하므로 Redis 블로킹 없음
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    String countStr = redisTemplate.opsForValue().get(key);
                    if (countStr == null) continue;

                    Long increment = Long.parseLong(countStr);
                    Long postId = Long.parseLong(key.substring(VIEW_COUNT_KEY_PREFIX.length()));

                    if (increment > 0) {
                        postRepository.increaseViews(postId, increment);
                    }
                    // increment <= 0 이어도 키 정리
                    redisTemplate.delete(key);

                } catch (NumberFormatException e) {
                    System.err.println("Error parsing Redis key or value: " + key);
                    redisTemplate.delete(key);
                } catch (Exception e) {
                    System.err.println("Error syncing view count for key " + key + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error during Redis scan: " + e.getMessage());
        }

        System.out.println("Redis to DB view sync finished.");
    }

    // 익일 자정 Instant 반환 (daily bucket 만료 시각 계산용)
    private Instant getNextMidnight() {
        return LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    // Redis 미플러시 delta 조회 헬퍼
    // useRedis=false 이거나 Redis 장애 시 0 반환
    private long getRedisViewDelta(Long postId) {
        if (!useRedis) return 0L;
        try {
            String val = redisTemplate.opsForValue().get(VIEW_COUNT_KEY_PREFIX + postId);
            return val != null ? Long.parseLong(val) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // 여러 postId 에 대해 Redis delta 를 한 번에 조회 (multiGet)
    // useRedis=false 이거나 Redis 장애 시 빈 Map 반환 → getOrDefault(id, 0L) 로 처리
    private Map<Long, Long> getRedisViewDeltas(List<Long> postIds) {
        if (!useRedis || postIds.isEmpty()) return Collections.emptyMap();
        try {
            List<String> keys = postIds.stream()
                    .map(id -> VIEW_COUNT_KEY_PREFIX + id)
                    .collect(Collectors.toList());
            List<String> values = redisTemplate.opsForValue().multiGet(keys);

            Map<Long, Long> result = new HashMap<>();
            for (int i = 0; i < postIds.size(); i++) {
                String val = (values != null) ? values.get(i) : null;
                if (val != null) {
                    result.put(postIds.get(i), Long.parseLong(val));
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // 댓글 개수 서포트 (솔직히 Comment Service 쪽에 있는 게 맞긴 한 듯하다.)
    @Transactional(readOnly = true)
    public Map<Long, Long> getCommentCountsByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = commentRepository.findCommentCountsByPostIds(postIds);
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],  // postId
                        result -> (Long) result[1],  // commentCount
                        (existing, replacement) -> existing
                ));
    }
}
