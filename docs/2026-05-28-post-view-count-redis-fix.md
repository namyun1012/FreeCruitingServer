# Post 조회수 Redis 정합성 수정 (2026-05-28)

## 문제

조회수를 Redis에 누적(`post:views:{id}`)하고 5분마다 DB에 플러시하는 구조였지만,
여러 문제가 있었다.

**1. 읽기 시 Redis delta 미반영**
Post 목록/상세 조회 시 Redis의 미플러시 delta를 반영하지 않고 DB 값만 내려줬다.
최대 5분간 실제보다 낮은 조회수가 노출됐다.

**2. expire 리셋 버그**
새 유저가 볼 때마다 `expire 24h`를 매번 리셋했다. 인기 게시글은
새 유저가 계속 들어오면서 `post:viewed_users:{id}` Set이 사실상 만료되지 않고
유저 ID를 무한 누적했다.

**3. 비로그인 유저 dedup 없음**
`useRedis=true` 여도 비로그인 유저는 dedup 없이 매 요청마다 DB를 직접 증가시켰다.
로그인/비로그인 간 동작이 불일치했다.

**4. `keys()` 블로킹**
`syncViewsFromRedisToDb()`에서 `redisTemplate.keys("post:views:*")`를 사용했다.
Redis는 싱글 스레드이므로 `KEYS`는 전체 키를 O(N) 스캔하는 동안 다른 모든 명령을 블로킹한다.

---

## 수정 내용

### 1. PostListResponseDto / PostResponseDto — `redisDelta` 생성자 추가

```java
// PostListResponseDto
public PostListResponseDto(Post entity, Long commentCount, long redisDelta) {
    this.views = (int) (entity.getViews() + redisDelta);
    ...
}

// PostResponseDto
public PostResponseDto(Post entity, long redisDelta) {
    this.views = (int) (entity.getViews() + redisDelta);
    ...
}
```

### 2. PostService — 조회수 읽기 시 Redis delta 합산

| 경로 | useRedis=false | useRedis=true |
|---|---|---|
| `findById` | DB 직접 증가 후 기존 생성자 | Redis delta 누적 후 `getRedisViewDelta(id)` 합산 |
| `findAllPage` / `findByType` / `search` | delta=0 | `multiGet` 배치 조회 후 합산 |

헬퍼 추가:
- `getRedisViewDelta(Long postId)` — 단건 조회. Redis 장애 시 0 반환.
- `getRedisViewDeltas(List<Long> postIds)` — `multiGet` 배치 조회. Redis 장애 시 빈 Map 반환 (N+1 없음).

### 3. dedup 구조 개선 — daily bucket + 식별자 통합

**식별자 통합 (Controller → Service)**

```
로그인:   "u:{userId}"     → 재로그인해도 동일, dedup 유지
비로그인: "s:{sessionId}"  → HttpServletRequest.getSession().getId()
```

IP 대신 세션 ID를 사용한 이유: IP는 개인정보보호법(PIPA)상 개인정보로 간주될 수 있다.
세션 ID는 개인 식별이 불가하며 이미 Spring Session으로 관리되는 값이다.

**daily bucket 키 구조**

```
변경 전: post:viewed_users:{postId}
         → 유저가 볼 때마다 expire 24h 리셋 → 인기글은 Set이 만료 안 됨

변경 후: post:viewed:{postId}:{yyyyMMdd}
         → 하루 단위로 키 분리 → Set 크기가 당일 UV 수준으로 자연 제한
         → expireAt 익일 자정 (절대 시각, 멱등 — 여러 번 호출해도 동일한 값)
```

```java
String today = LocalDate.now().format(DATE_FORMATTER); // "20260528"
String viewedKey = VIEWED_KEY_PREFIX + id + ":" + today;

Long addedCount = redisTemplate.opsForSet().add(viewedKey, identifier);
if (addedCount != null && addedCount > 0) {
    redisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + id);
    redisTemplate.expireAt(viewedKey, getNextMidnight()); // 익일 자정
}
```

### 4. `syncViewsFromRedisToDb` — `keys()` → `scan()` 전환

```
before: redisTemplate.keys("post:views:*")
        → Redis 전체를 O(N) 블로킹 스캔

after:  redisTemplate.scan(ScanOptions)
        → 커서 기반 반복, Redis 블로킹 없음
        → try-with-resources 로 커서 자원 보장
        → Cursor<String> (이 프로젝트의 RedisTemplate은 String 직렬화 사용)
```

`REDIS_SCAN_COUNT = 100` 상수로 분리.
Redis에게 커서 이동당 반환 키 수의 힌트를 제공하며 Redis가 정확히 보장하지는 않는다.
게시글이 많아질 경우 튜닝 가능.

---

## 변경 파일

- `src/main/java/com/project/freecruting/dto/post/PostListResponseDto.java`
- `src/main/java/com/project/freecruting/dto/post/PostResponseDto.java`
- `src/main/java/com/project/freecruting/service/PostService.java`
- `src/main/java/com/project/freecruting/controller/PostController.java`
- `src/main/java/com/project/freecruting/controller/IndexController.java`
- `src/test/java/com/project/freecruting/controller/PostControllerTest.java`
- `src/test/java/com/project/freecruting/service/PostServiceTest.java`
