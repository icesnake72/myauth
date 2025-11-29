# 인증 시스템 보안 개선 TODO

## 🔴 Critical - 즉시 개선 필요

### 1. Access Token 무효화 메커니즘 구현
- [ ] Redis 기반 Token Blacklist 구현
  - 로그아웃 시 Access Token을 Redis Blacklist에 추가
  - JwtAuthenticationFilter에서 Blacklist 확인 로직 추가
  - TTL은 Access Token 만료시간과 동일하게 설정
- [ ] 대안: Access Token 유효시간 단축 (1시간 → 5-15분)
  - JwtTokenProvider의 accessTokenValidity 조정
  - 프론트엔드에서 자동 갱신 로직 구현 필요

**현재 문제:** 로그아웃 후에도 Access Token은 만료(1시간)까지 유효하여 탈취된 토큰으로 API 접근 가능

**구현 힌트:**
```java
// RedisTokenBlacklistService 생성
// - void addToBlacklist(String token, long expirySeconds)
// - boolean isBlacklisted(String token)
```

---

### 2. JWT Secret Key 보안 강화
- [ ] application.properties에서 Secret Key 제거
- [ ] 환경변수로 Secret Key 관리
  - application.yml에 `${JWT_SECRET_KEY}` 사용
  - 서버 실행 시 환경변수 설정
- [ ] (선택) AWS KMS, HashiCorp Vault 등 Key Management Service 도입

**현재 문제:** Secret Key가 평문으로 저장되어 코드 저장소 유출 시 모든 토큰 위조 가능

**구현 예시:**
```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET_KEY:default-key-for-dev}
  access-token-validity: ${JWT_ACCESS_VALIDITY:3600000}
```

---

### 3. Rate Limiting 구현
- [ ] 로그인 엔드포인트에 Rate Limiting 적용
  - IP당 분당 5회 제한
  - 계정당 분당 3회 제한
- [ ] Refresh Token 갱신에 Rate Limiting 적용
  - IP당 분당 10회 제한
- [ ] 회원가입에 Rate Limiting 적용

**현재 문제:** Brute Force 공격, DDoS 공격에 취약

**구현 방법:**
- Spring Boot Bucket4j + Redis
- 또는 Custom RateLimitingFilter 구현
- 또는 API Gateway (Nginx, Kong) 레벨에서 처리

---

## 🟠 High - 우선 개선 권장

### 4. Refresh Token Rotation 구현
- [ ] Refresh Token 사용 시 새 Refresh Token 발급
- [ ] 기존 Refresh Token은 즉시 무효화
- [ ] One-time use Refresh Token 정책 적용
- [ ] (선택) Refresh Token Family 추적으로 탈취 감지

**현재 문제:** Refresh Token이 7일간 계속 재사용되어 토큰 탈취 시 장기간 악용 가능

**구현 위치:** AuthService.refreshAccessToken() 메서드 수정

---

### 5. 보안 감사 로그 구현
- [ ] audit_logs 테이블 생성
  ```sql
  CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(50),  -- LOGIN, LOGOUT, REFRESH, FAILED_LOGIN
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_info TEXT,
    success BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  ```
- [ ] AuditLog 엔티티 생성
- [ ] AuditLogService 구현
- [ ] 로그인/로그아웃/Refresh 시 로그 기록

**현재 문제:** 보안 사고 발생 시 추적 불가

---

### 6. 동시 로그인 제한
- [ ] refresh_tokens 테이블에 device_id 컬럼 추가
- [ ] 디바이스 핑거프린트 생성 로직 구현
- [ ] 계정당 최대 디바이스 수 제한 (예: 5개)
- [ ] 오래된 디바이스 자동 로그아웃

**현재 문제:** 한 계정으로 무제한 디바이스 로그인 가능 → 계정 공유, 토큰 탈취 위험

**구현 힌트:**
```java
// 로그인 시
// 1. 현재 활성 디바이스 수 확인
// 2. 최대 개수 초과 시 가장 오래된 디바이스의 Refresh Token 삭제
// 3. 새 디바이스 등록
```

---

### 7. IP 주소/Device 검증 구현
- [ ] refresh_tokens 테이블에 last_ip, device_fingerprint 컬럼 추가
- [ ] Refresh Token 사용 시 IP 주소 변경 감지
- [ ] IP 변경 시 옵션:
  - 재인증 요구
  - 이메일/SMS 알림 발송
  - 의심스러운 활동으로 로그 기록

**현재 문제:** 토큰 탈취 시 다른 위치/디바이스에서 사용 가능

---

## 🟡 Medium - 점진적 개선

### 8. HTTPS 강제 및 보안 헤더 추가
- [ ] Production 환경에서 HTTPS 강제
  ```java
  // SecurityConfig에 추가
  .requiresChannel(channel -> channel.anyRequest().requiresSecure())
  ```
- [ ] HSTS (HTTP Strict Transport Security) 헤더 추가
- [ ] Security Headers 추가
  - X-Content-Type-Options: nosniff
  - X-Frame-Options: DENY
  - X-XSS-Protection: 1; mode=block

**현재 문제:** HTTP 통신 허용 시 중간자 공격으로 토큰 탈취 가능

---

### 9. CORS 설정 점검 및 강화
- [ ] 현재 CORS 설정 확인
- [ ] 허용 Origin을 명시적으로 지정 (와일드카드 제거)
- [ ] 허용 메서드, 헤더 최소화
- [ ] credentials 설정 확인

**구현 예시:**
```java
@Configuration
public class CorsConfig {
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    // ...
  }
}
```

---

### 10. Access Token 유효시간 단축
- [ ] 현재 1시간 → 5-15분으로 단축
- [ ] 프론트엔드에서 자동 갱신 로직 구현
- [ ] Silent Refresh 패턴 적용

**현재 문제:** 1시간은 일반적으로 긴 편, 토큰 탈취 시 악용 시간 증가

---

### 11. Refresh Token 슬라이딩 윈도우 구현
- [ ] Refresh Token 사용 시 만료시간 연장
- [ ] 예: 사용 시마다 +7일 연장 (최대 30일)
- [ ] 장기 미사용 시 자동 만료

**현재 문제:** 7일 후 무조건 만료되어 활성 사용자도 강제 재로그인

---

## 🟢 Low - 장기적 개선

### 12. MFA (Multi-Factor Authentication) 구현
- [ ] TOTP 기반 OTP 구현 (Google Authenticator 호환)
- [ ] SMS OTP 구현
- [ ] 이메일 인증 코드 구현
- [ ] MFA 활성화/비활성화 기능
- [ ] 백업 코드 생성

**라이브러리:** `com.google.zxing:core` (QR 코드), `de.taimos:totp` (TOTP)

---

### 13. 비밀번호 정책 강화
- [ ] 비밀번호 복잡도 검증
  - 최소 8자 이상
  - 대소문자, 숫자, 특수문자 포함
- [ ] 비밀번호 히스토리 관리 (최근 5개 재사용 방지)
- [ ] 주기적 비밀번호 변경 권장 (90일)
- [ ] 비밀번호 만료 정책

**라이브러리:** `org.passay:passay` (비밀번호 검증)

---

### 14. 계정 잠금 정책 강화
- [ ] 현재 failed_login_attempts 필드 활용 확인
- [ ] 5회 로그인 실패 시 15분 잠금
- [ ] 10회 실패 시 계정 비활성화 (관리자 해제 필요)
- [ ] 성공적인 로그인 시 실패 횟수 초기화

**구현 위치:** CustomUserDetailsService 또는 AuthService

---

## 우선순위 개선 로드맵

### Phase 1 - 즉시 구현 (1-2주)
1. ✅ Token Blacklist (Redis)
2. ✅ Secret Key 환경변수화
3. ✅ Rate Limiting

### Phase 2 - 단기 구현 (1개월)
4. ✅ Refresh Token Rotation
5. ✅ Audit Logging
6. ✅ 동시 로그인 제한

### Phase 3 - 중기 구현 (3개월)
7. ✅ IP/Device 검증
8. ✅ HTTPS 강제
9. ✅ Access Token 단축
10. ✅ CORS 강화

### Phase 4 - 장기 구현 (6개월+)
11. ✅ MFA (2FA)
12. ✅ 고급 비밀번호 정책
13. ✅ 계정 잠금 정책 고도화

---

## 추가 고려사항

### Redis 도입
- Token Blacklist
- Rate Limiting
- Session 관리 (필요시)

### 모니터링 및 알림
- [ ] 의심스러운 로그인 감지 시 알림
- [ ] 여러 위치에서 동시 로그인 감지
- [ ] 비정상적인 API 호출 패턴 감지

### 성능 최적화
- [ ] JWT 검증 캐싱
- [ ] DB 쿼리 최적화
- [ ] 인덱스 추가

---

**작성일:** 2025-11-30
**버전:** 1.0
**다음 리뷰:** 매주 월요일
