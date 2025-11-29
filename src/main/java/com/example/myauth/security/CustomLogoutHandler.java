package com.example.myauth.security;

import com.example.myauth.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 로그아웃 핸들러
 * 로그아웃 시 Refresh Token을 DB에서 삭제하여 완전히 revoke 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 로그아웃 처리
   * 1. Authorization 헤더 또는 쿠키에서 Access Token 추출
   * 2. Access Token에서 사용자 이메일 추출
   * 3. 해당 사용자의 모든 Refresh Token을 DB에서 삭제
   * 4. 쿠키가 있다면 제거 (웹 클라이언트)
   *
   * @param request HTTP 요청 객체
   * @param response HTTP 응답 객체
   * @param authentication 인증 정보 (null일 수 있음)
   */
  @Override
  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    log.info("로그아웃 처리 시작");

    // 1️⃣ Access Token 추출
    String accessToken = extractAccessToken(request);

    if (accessToken == null) {
      log.warn("Access Token이 없어 로그아웃 처리를 건너뜁니다");
      return;
    }

    // 2️⃣ Access Token 검증 및 이메일 추출
    try {
      if (!jwtTokenProvider.validateToken(accessToken)) {
        log.warn("유효하지 않은 Access Token으로 로그아웃 시도");
        // 유효하지 않아도 쿠키는 삭제해야 함
        clearRefreshTokenCookie(response);
        return;
      }

      String email = jwtTokenProvider.getEmailFromToken(accessToken);
      log.info("로그아웃 요청: {}", email);

      // 3️⃣ 해당 사용자의 모든 Refresh Token을 DB에서 삭제
      int deletedCount = refreshTokenRepository.deleteByUserEmail(email);
      log.info("Refresh Token 삭제 완료: {} (삭제된 토큰 수: {})", email, deletedCount);

      // 4️⃣ 쿠키 삭제 (웹 클라이언트)
      clearRefreshTokenCookie(response);

    } catch (Exception e) {
      log.error("로그아웃 처리 중 오류 발생: {}", e.getMessage(), e);
      // 오류가 발생해도 쿠키는 삭제
      clearRefreshTokenCookie(response);
    }
  }

  /**
   * HTTP 요청에서 Access Token을 추출한다
   * Authorization 헤더에서 Bearer 토큰을 찾는다
   *
   * @param request HTTP 요청 객체
   * @return Access Token (없으면 null)
   */
  private String extractAccessToken(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }

    return null;
  }

  /**
   * Refresh Token 쿠키를 제거한다
   * 웹 클라이언트의 경우 쿠키에 저장된 Refresh Token을 삭제해야 함
   *
   * @param response HTTP 응답 객체
   */
  private void clearRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie("refreshToken", null);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(0);  // 즉시 만료
    response.addCookie(cookie);
    log.debug("Refresh Token 쿠키 삭제 완료");
  }
}
