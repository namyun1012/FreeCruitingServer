# CSRF 보호 설정

## 개요

세션 기반 인증(Redis-backed Spring Session)을 사용하는 구조에서 CSRF 보호를 활성화했다.
향후 React 프론트엔드로 전환할 때 백엔드 변경 없이 그대로 사용할 수 있도록 **Double Submit Cookie 패턴**을 적용했다.

---

## 방식: Double Submit Cookie (CookieCsrfTokenRepository)

```
1. 서버 → 브라우저: XSRF-TOKEN 쿠키 발급 (non-HttpOnly, JS에서 읽기 가능)
2. 브라우저 → 서버: X-XSRF-TOKEN 헤더에 쿠키 값을 담아서 전송
3. 서버: 쿠키 값과 헤더 값 비교 → 일치하면 허용
```

### 기존 방식(csrf.disable())과 비교

| | csrf.disable() | CookieCsrfTokenRepository |
|--|--|--|
| 세션 + 쿠키 인증 | 취약 | 안전 |
| React 전환 시 백엔드 변경 | - | 불필요 |
| Thymeleaf 자동 토큰 주입 | - | 해당 없음 (Mustache 사용) |

---

## 변경 파일

### 백엔드

**`config/auth/SecurityConfig.java`**

```java
// 변경 전
.csrf(csrf -> csrf.disable())

// 변경 후
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
)
.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
.logout(logout -> logout
    .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // GET/POST 모두 허용
    .logoutSuccessUrl("/")
    .invalidateHttpSession(true)
)
```

**CsrfCookieFilter** (SecurityConfig 내부 static class)

Spring Security 6의 `CookieCsrfTokenRepository`는 토큰을 지연 로드(lazy)한다.
OAuth2 로그인 성공 후 redirect 응답처럼 토큰이 참조되지 않는 경우 `XSRF-TOKEN` 쿠키가 발급되지 않는 문제가 있다.
이 필터가 모든 요청마다 `csrfToken.getToken()`을 강제 호출해 쿠키를 항상 발급되도록 한다.

```java
static final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // lazy token 강제 로드 → 쿠키 기록
        }
        filterChain.doFilter(request, response);
    }
}
```

> 쿠키 읽기/쓰기 수준의 작업이므로 성능 영향 없음.
> 정적 파일(`/js/**`, `/css/**` 등)은 `WebSecurityCustomizer`로 필터 체인 자체를 타지 않아 실행되지 않는다.

**로그아웃 GET 허용**

기존 로그아웃 링크가 `<a href="/logout">` (GET)이므로 `AntPathRequestMatcher("/logout")`로 메서드 무관하게 허용한다.
최악의 경우 공격자가 피해자를 강제 로그아웃시킬 수 있으나, 데이터 손실이 없어 위험도가 낮다.

### 프론트엔드

**`static/js/csrf.js`** (신규)

모든 페이지에서 공통으로 사용하는 토큰 읽기 유틸 함수.
`footer.mustache`를 통해 전역 로드된다.

```javascript
function getCsrfToken() {
    var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
}
```

**`static/index.js`**

jQuery AJAX 전체에 CSRF 헤더를 자동 적용한다.
`$.ajaxSetup`은 전역 설정이므로 `contest.js` 등 다른 파일의 `$.ajax` 호출에도 적용된다.

```javascript
$.ajaxSetup({
    beforeSend: function(xhr) {
        var token = getCsrfToken();
        if (token) {
            xhr.setRequestHeader('X-XSRF-TOKEN', token);
        }
    }
});
```

**`static/js/notification.js`**

fetch API는 `$.ajaxSetup` 적용 대상이 아니므로 PATCH 요청 2곳에 직접 헤더를 추가했다.

```javascript
// markAsRead, markAllAsRead
headers: { 'X-XSRF-TOKEN': getCsrfToken() }
```

**`templates/login.mustache`**

폼 POST 제출은 헤더가 아닌 파라미터로 토큰을 전달해야 한다.
submit 이벤트에서 쿠키 값을 읽어 hidden input에 주입한다.

```html
<form action="/login" method="post" id="loginForm">
    <input type="hidden" id="csrf-token-input" name="_csrf" value="">
    ...
</form>
<script>
    document.getElementById('loginForm').addEventListener('submit', function() {
        var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
        var token = match ? decodeURIComponent(match[1]) : '';
        document.getElementById('csrf-token-input').value = token;
    });
</script>
```

**`templates/layout/footer.mustache`**

`csrf.js`를 `index.js`보다 먼저 로드한다.

```html
<script src="/js/csrf.js"></script>
<script src="/index.js"></script>
```

**`templates/contest-list.mustache`, `templates/contest-read.mustache`**

footer를 포함하지 않는 독립 구조였다. footer로 전환하여 `csrf.js`와 `$.ajaxSetup`이 적용되도록 했다.
`contest.js`는 jQuery 의존성 때문에 footer 이후에 로드한다.

```html
{{>layout/footer}}
<script src="/js/contest.js"></script>
```

---

## React 전환 시 대응 방법

백엔드는 변경 불필요. React 앱에서 아래 패턴만 적용하면 된다.

```javascript
// 쿠키에서 토큰 읽기
function getCsrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
}

// 모든 mutating 요청에 헤더 추가
fetch('/api/v1/posts', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': getCsrfToken()
    },
    credentials: 'include',
    body: JSON.stringify(data)
});
```

axios를 사용한다면 인터셉터로 전역 적용 가능하다.

```javascript
axios.interceptors.request.use(config => {
    const token = getCsrfToken();
    if (token) config.headers['X-XSRF-TOKEN'] = token;
    return config;
});
```

---

## 주의 사항

- `GET`, `HEAD`, `OPTIONS`, `TRACE` 요청은 Spring Security가 CSRF 검증을 하지 않는다.
- `POST`, `PUT`, `DELETE`, `PATCH` 요청은 반드시 `X-XSRF-TOKEN` 헤더(또는 `_csrf` 파라미터)가 필요하다.
- `XSRF-TOKEN` 쿠키는 `HttpOnly=false`이므로 XSS 공격에 노출될 경우 탈취 가능하다. XSS 방어도 함께 신경써야 한다.
