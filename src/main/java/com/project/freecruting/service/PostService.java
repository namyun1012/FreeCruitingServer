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

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.use-redis-for-views:false}")
    private boolean useRedis;

    private static final String VIEW_COUNT_KEY_PREFIX = "post:views:";
    private static final String VIEWED_USERS_KEY_PREFIX = "post:viewed_users:";

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
    public PostResponseDto findById(Long id, Long user_id) {
        Post entity = postRepository.findById(id).orElseThrow(() -> new NotFoundException("해당 게시글 없음. id=" + id));

        // redis 사용시
        if(useRedis && user_id != null) {
            try {
//            String key = VIEW_COUNT_KEY_PREFIX + id;
//            redisTemplate.opsForValue().increment(key);
            String userSetKey = VIEWED_USERS_KEY_PREFIX + id;
            Long addedCount = redisTemplate.opsForSet().add(userSetKey, user_id.toString());

            if (addedCount != null && addedCount > 0) {
                String viewCountKey = VIEW_COUNT_KEY_PREFIX + id;
                redisTemplate.opsForValue().increment(viewCountKey);

                redisTemplate.expire(userSetKey, Duration.ofHours(24));
            }
            }
            catch (Exception e) {
                System.out.println(e);
            }

        }
        
        // redis 미사용시, 로그인 안한 유저들 포함
        else {
            postRepository.increaseViews(id);
        }

        return new PostResponseDto(entity);
    }

    public PostResponseDto findByIdForUpdate(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("해당 게시글 없음"));

        if(!post.getAuthor_id().equals(userId)) {
            throw new ForbiddenException("작성자만 수정 가능");
        }

        return new PostResponseDto(post);

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

        return postRepository.findAllByOrderByIdDesc(pageable)
                .map(PostListResponseDto::new);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> findByType(String type, int page, int size) {
        Post.PostType postType = Post.PostType.fromString(type);
        Pageable pageable = PageRequest.of(page, size);


        return postRepository.findByTypeOrderByIdDesc(pageable, postType)
                .map(PostListResponseDto:: new);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> search(String query, SearchType searchType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostListResponseDto> result;

        if(searchType == SearchType.ALL) {
            result = postRepository.findByTitleOrContentOrAuthor(query, query, query, pageable).map(PostListResponseDto::new);
        }

        else if(searchType == SearchType.TITLE) {
            result = postRepository.findByTitle(query, pageable).map(PostListResponseDto::new);
        }

        else if(searchType == SearchType.CONTENT) {
            result = postRepository.findByContent(query, pageable).map(PostListResponseDto::new);
        }

        else if(searchType == SearchType.AUTHOR) {
            result = postRepository.findByAuthor(query, pageable).map(PostListResponseDto::new);
        }
        
        // 이상한 값일 때  검색
        else {
            result = postRepository.findByTitleOrContentOrAuthor(query, query, query, pageable).map(PostListResponseDto::new);
        }

        return result;
    }

    // Support Function
    // post view redis schedule
    @Scheduled(fixedRateString = "${app.view-counting.sync-interval-ms:300000}")
    @Transactional
    public void syncViewsFromRedisToDb() {

        if(!useRedis) return;

        String pattern = VIEW_COUNT_KEY_PREFIX + "*";
        System.out.println("Attempting to get Redis keys with pattern: " + pattern);
        // 주의: keys() 명령어는 Redis에 데이터가 많을 경우 블록킹될 수 있습니다.
        // 운영 환경에서는 성능을 위해 scan() 명령어를 사용하는 것을 고려하세요.
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return;
        }

        System.out.println("Redis to DB view sync started.");
        for (String key : keys) {
            try {

                // Redis에서 현재 조회수 값 가져오기
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr == null) {
                    // Key might have expired or been deleted between keys() and get()
                    continue;
                }

                Long increment = Long.parseLong(countStr);
                String postIdStr = key.substring(VIEW_COUNT_KEY_PREFIX.length());
                Long postId = Long.parseLong(postIdStr);

                if (increment > 0) {
                    // 데이터베이스 업데이트: 현재 DB 조회수에 Redis 값을 더함
                    postRepository.increaseViews(postId, increment);

                    // DB 업데이트 성공 후 Redis 키 삭제 (원자적 작업은 아님)
                    // 만약 DB 업데이트 실패 시 Redis 키는 남아서 다음 스케줄러에 재시도됩니다.
                    redisTemplate.delete(key);
                } else {
                    // 값이 0 이거나 음수(increment 사용 시 발생하면 안 됨)면 키 삭제
                    redisTemplate.delete(key);
                }

            } catch (NumberFormatException e) {
                System.err.println("Error parsing Redis key or value: " + key + ", value: " + redisTemplate.opsForValue().get(key));
                redisTemplate.delete(key);
            } catch (Exception e) {
                System.err.println("Error syncing view count for key " + key + ": " + e.getMessage());
            }
        }
        System.out.println("Redis to DB view sync finished.");
    }
}
