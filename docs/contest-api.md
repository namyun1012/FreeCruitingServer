# Contest (공모전) API 문서

## 개요

공모전 정보를 등록·관리하고, 사용자가 탐색할 수 있는 도메인입니다.
등록/수정/삭제는 로그인 사용자(USER 롤)만 가능하며, 조회는 비로그인 포함 누구나 가능합니다.

> **TODO**: 추후 ADMIN 롤 도입 시 쓰기 엔드포인트를 `hasRole("ADMIN")`으로 교체 예정.

---

## 파일 구조

```
model/
  Contest.java                          엔티티
  type/
    ContestCategory.java                분야 enum
    ContestStatus.java                  상태 enum (날짜 기반 자동 계산)

dto/contest/
  ContestSaveRequestDto.java            등록 요청
  ContestUpdateRequestDto.java          수정 요청
  ContestResponseDto.java               단건 상세 응답
  ContestListResponseDto.java           목록 응답 (요약)

repository/
  ContestRepository.java                JPA + 커스텀 쿼리

service/
  ContestService.java                   비즈니스 로직 + Redis 조회수

controller/
  ContestController.java                REST API (/api/v1/contests)
```

---

## 도메인 모델

### Entity: `Contest`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | `Long` | PK, auto | - |
| `title` | `String` | 최대 500자, not null | 공모전 명 |
| `description` | `String` | TEXT | 상세 설명 |
| `organizer` | `String` | not null | 주최기관 |
| `category` | `ContestCategory` | not null | 분야 |
| `applicationStartDate` | `LocalDate` | not null | 접수 시작일 |
| `applicationDeadline` | `LocalDate` | not null | 접수 마감일 |
| `contestStartDate` | `LocalDate` | nullable | 대회 시작일 |
| `contestEndDate` | `LocalDate` | nullable | 대회 종료일 |
| `imageUrl` | `String` | nullable | 배너 이미지 URL |
| `officialUrl` | `String` | 최대 1000자, nullable | 공식 사이트 링크 |
| `target` | `String` | nullable | 참가 대상 (자유 텍스트) |
| `region` | `String` | nullable | 지역 (예: "온라인", "서울") |
| `views` | `int` | default 0 | 조회수 |
| `createdDate` | `LocalDateTime` | auto (`BaseTimeEntity`) | 생성일시 |
| `modifiedDate` | `LocalDateTime` | auto (`BaseTimeEntity`) | 수정일시 |

**`getStatus()`** — DB 컬럼 없이 날짜로 자동 계산되는 메서드:

```
applicationStartDate 이전          → UPCOMING   (접수 전)
applicationStartDate ~ deadline    → RECRUITING  (접수 중)
deadline 이후 ~ contestEndDate     → IN_PROGRESS (진행 중)
contestEndDate 이후 (또는 날짜 미설정) → CLOSED  (종료)
```

---

### Enum: `ContestCategory`

| 값 | 설명 |
|----|------|
| `PROGRAMMING` | 개발/IT |
| `DESIGN` | 디자인 |
| `STARTUP` | 창업/아이디어 |
| `ART` | 예술/공연 |
| `SOCIAL` | 사회공헌 |
| `ESSAY` | 논문/에세이 |
| `VIDEO` | 영상/사진 |
| `MARKETING` | 마케팅/광고 |
| `OTHER` | 기타 |

### Enum: `ContestStatus`

| 값 | 설명 |
|----|------|
| `UPCOMING` | 접수 전 |
| `RECRUITING` | 접수 중 |
| `IN_PROGRESS` | 진행 중 |
| `CLOSED` | 종료 |

---

## API 엔드포인트

Base path: `/api/v1/contests`

### GET `/api/v1/contests`
공모전 목록 조회 (페이지네이션, 필터, 검색)

**인증**: 불필요

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| `category` | N | - | 분야 필터 (ContestCategory 값) |
| `keyword` | N | - | 키워드 검색 (title / organizer / description) |
| `page` | N | `0` | 페이지 번호 |
| `size` | N | `10` | 페이지 크기 |

> `keyword`와 `category`가 동시에 있으면 `keyword` 우선.

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "title": "2026 AI 공모전",
      "organizer": "과학기술정보통신부",
      "category": "PROGRAMMING",
      "applicationDeadline": "2026-06-30",
      "imageUrl": "https://...",
      "target": "대학(원)생",
      "region": "온라인",
      "status": "RECRUITING",
      "views": 142,
      "modifiedDate": "2026-05-01T12:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

---

### GET `/api/v1/contests/{id}`
공모전 단건 상세 조회 + 조회수 증가

**인증**: 불필요 (로그인 시 Redis 24h 중복 방지 적용)

**Response** `200 OK`
```json
{
  "id": 1,
  "title": "2026 AI 공모전",
  "description": "AI 기술을 활용한 ...",
  "organizer": "과학기술정보통신부",
  "category": "PROGRAMMING",
  "applicationStartDate": "2026-05-01",
  "applicationDeadline": "2026-06-30",
  "contestStartDate": "2026-07-15",
  "contestEndDate": "2026-09-30",
  "imageUrl": "https://...",
  "officialUrl": "https://contest.example.com",
  "target": "대학(원)생",
  "region": "온라인",
  "status": "RECRUITING",
  "views": 143,
  "createdDate": "2026-04-20T09:00:00",
  "modifiedDate": "2026-05-01T12:00:00"
}
```

**Error** `404` — 존재하지 않는 id

---

### POST `/api/v1/contests`
공모전 등록

**인증**: USER 롤 필요

**Request Body**
```json
{
  "title": "2026 AI 공모전",
  "description": "AI 기술을 활용한 ...",
  "organizer": "과학기술정보통신부",
  "category": "PROGRAMMING",
  "applicationStartDate": "2026-05-01",
  "applicationDeadline": "2026-06-30",
  "contestStartDate": "2026-07-15",
  "contestEndDate": "2026-09-30",
  "imageUrl": "https://...",
  "officialUrl": "https://contest.example.com",
  "target": "대학(원)생",
  "region": "온라인"
}
```

> `contestStartDate`, `contestEndDate`, `imageUrl`, `officialUrl`, `target`, `region` 은 선택 항목.

**Response** `200 OK`
```json
{ "id": 1 }
```

---

### PUT `/api/v1/contests/{id}`
공모전 수정

**인증**: USER 롤 필요

**Request Body**: POST와 동일한 구조

**Response** `200 OK`
```json
{ "message": "Update successful" }
```

**Error** `404` — 존재하지 않는 id

---

### DELETE `/api/v1/contests/{id}`
공모전 삭제

**인증**: USER 롤 필요

**Response** `200 OK`
```json
{ "message": "Delete successful" }
```

**Error** `404` — 존재하지 않는 id

---

## 조회수 처리 (Redis)

`application.yml`의 `app.use-redis-for-views` 값에 따라 동작이 달라집니다.

### Redis 활성화 시 (`true`)

```
단건 조회 요청
  └─ userId 존재 →  Redis Set (contest:viewed_users:{id}) 에 userId 추가
       ├─ 새로운 userId → Redis counter (contest:views:{id}) increment + TTL 24h 설정
       └─ 중복 userId   → 조회수 변화 없음
  └─ userId 없음  →  DB 직접 increment (비로그인 사용자)

@Scheduled(fixedRate = 5분)
  └─ contest:views:* 키를 순회하여 DB에 일괄 반영 후 키 삭제
```

### Redis 비활성화 시 (`false`)
모든 조회에서 DB를 직접 increment합니다.

### Redis Key 구조

| Key | 타입 | 설명 |
|-----|------|------|
| `contest:views:{id}` | String (counter) | 누적 조회수 (DB 반영 전) |
| `contest:viewed_users:{id}` | Set | 24시간 내 조회한 userId 목록 |

---

## Security 설정 요약

```
GET  /api/v1/contests/**  →  permitAll  (비로그인 허용)
기타 /api/v1/contests/**  →  hasRole("USER")
```

[SecurityConfig.java](../src/main/java/com/project/freecruting/config/auth/SecurityConfig.java) 참고.
