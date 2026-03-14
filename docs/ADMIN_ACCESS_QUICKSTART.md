# Admin 기능 연동 퀵스타트 (URL/접속 방법)

이 문서는 현재 백엔드 구현 기준으로 Admin 기능을 바로 붙이기 위한 최소 절차를 정리한다.

## 1. URL 정리

## 1.1 로컬 개발

- 백엔드 베이스: `http://localhost:9080`
- 헬스체크: `GET http://localhost:9080/api/health`
- Admin API 베이스: `http://localhost:9080/api/admin`
- React(Admin 페이지) 권장 라우트: `http://localhost:5173/admin`

예시 API:
- `GET http://localhost:9080/api/admin/dashboard/stats`
- `GET http://localhost:9080/api/admin/users?page=0&size=20`

## 1.2 운영(nginx 경유)

- 프론트: `http://<SERVER_IP>/admin`
- Admin API: `http://<SERVER_IP>/api/admin/...`

주의:
- Nginx가 `/api`를 Spring Boot로 프록시해야 한다.
- 브라우저 쿠키 연동을 위해 프론트와 API 도메인은 동일 출처 구성이 가장 안전하다.

## 2. 관리자 계정 생성 방법

기본 관리자 계정/패스워드는 코드에 없다.

1) 일반 회원가입
```bash
curl -X POST http://localhost:9080/api/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password1234","username":"admin"}'
```

2) DB에서 관리자 승격
```sql
UPDATE users
SET role = 'ROLE_ADMIN',
    status = 'ACTIVE',
    is_active = 1
WHERE email = 'admin@example.com';
```

선택: 슈퍼유저 권한까지 필요하면
```sql
UPDATE users
SET role = 'ROLE_ADMIN',
    is_super_user = 1,
    status = 'ACTIVE',
    is_active = 1
WHERE email = 'admin@example.com';
```

## 3. 로그인/접속 검증

1) 로그인 (Access Token 발급)
```bash
curl -X POST http://localhost:9080/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password1234"}'
```

2) 응답의 `data.accessToken` 추출 후 Admin API 호출
```bash
curl http://localhost:9080/api/admin/dashboard/stats \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

성공 시 `success: true` 응답.
권한 없으면 403 + `관리자 권한이 필요합니다.`

## 4. React 연동 최소 코드

```js
// axios instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9080',
  withCredentials: true,
});

// 로그인 후
api.defaults.headers.common.Authorization = `Bearer ${accessToken}`;

// Admin API 호출
const { data } = await api.get('/api/admin/dashboard/stats');
```

## 5. 프론트 접속 방법 (사용자 기준)

1) `/admin/login` 또는 기존 로그인 페이지에서 관리자 계정으로 로그인
2) 로그인 성공 후 `/admin/dashboard` 이동
3) 초기 로딩 시 아래 3개 API 호출
- `/api/admin/dashboard/stats`
- `/api/admin/dashboard/recent-users`
- `/api/admin/dashboard/recent-posts`

4) 메뉴 진입 URL
- 사용자 관리: `/admin/users`
- 게시글 관리: `/admin/posts`
- 댓글 관리: `/admin/comments`

## 6. 자주 틀리는 포인트

- `/api/admin/**`는 `ROLE_ADMIN` 없으면 무조건 403
- 강제 로그아웃 응답 키는 `revokedRefreshTokens`
- 게시글 목록의 `imageCount`는 null 가능 (상세에서만 채워질 수 있음)
- `withCredentials: true` 누락 시 refresh 쿠키 연동 실패
