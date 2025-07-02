# 🧑‍💻 FreeCruiting - 스터디 / 프로젝트 팀원 모집 커뮤니티

**FreeCruiting**는 개발자, 디자이너, 기획자 등 다양한 사람들이 함께 스터디나 사이드 프로젝트를 시작하고 팀원을 쉽게 모집할 수 있도록 돕는 커뮤니티 웹 애플리케이션입니다.

> Java + Spring Boot로 백엔드를 구축했으며, 효율적인 팀원 모집을 위한 게시판, 댓글 기능, 파티 운영 등을 제공합니다.

---

## 📌 주요 기능

- ✅ **회원가입 및 로그인** (Session 기반 인증)
- 🗂️ **Google Oauth 로그인**
- 📝 **스터디/프로젝트 모집 게시글 등록, 수정, 삭제**
- 📬 **댓글 작성 및 관리 기능**
- 🏷️ **파티 생성 및 관리 기능**
- ❤️ **파티 신청 및 승인 거부 기능**
---

## ⚙️ 기술 스택

| 구분       | 기술 |
|------------|------|
| Language   | Java 17 |
| Framework  | Spring Boot 3.x |
| DB         | PostgreSQL, Redis|
| ORM        | Spring Data JPA (Hibernate) |
| Build Tool | Gradle |
| Security   | Spring Security + Session |
| View       | Mustache|
| Dev Tools  | IntelliJ, Git, Jmeter |

---

## 🗂️ 프로젝트 구조
```plaintext
src
└── main
    ├── java
    │   └── com.project.freecruiting
    │       ├── controller     # HTTP 요청 처리
    │       ├── service        # 비즈니스 로직
    │       ├── repository     # DB 접근 레이어 (JPA, Query 등)
    │       ├── model          # Entity 클래스
    │       ├── dto            # 요청/응답 데이터 형식
    │       ├── config         # 보안 및 전역 설정
    │       ├── handler        # 전역 예외 처리 핸들러
    │       ├── exception      # 커스텀 예외 클래스
    │       └── log            # 로깅 관련 처리
    └── resources
        ├── templates          # 뷰 템플릿 (Mustache 등)
        └── application.properties  # 환경 설정
---
