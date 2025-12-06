package com.example.myauth.controller;

import com.example.myauth.config.AppProperties;
import com.example.myauth.dto.kakao.KakaoOAuthDto;
import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.service.KakaoOAuthService;
import com.example.myauth.util.ClientTypeDetector;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 카카오 OAuth 로그인 컨트롤러
 * 카카오 소셜 로그인 엔드포인트를 제공
 */
@Slf4j
@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

  private final KakaoOAuthService kakaoOAuthService;
  private final AppProperties appProperties;

  /**
   * 카카오 로그인 시작
   * 사용자를 카카오 로그인 페이지로 리다이렉트
   *
   * GET /auth/kakao/login
   */
  @GetMapping("/login")
  public void kakaoLogin(HttpServletResponse response) throws IOException {
    log.info("카카오 로그인 요청");

    // 카카오 인가 코드 요청 URL 생성
    String authorizationUrl = kakaoOAuthService.getAuthorizationUrl();

    log.info("카카오 인가 페이지로 리다이렉트: {}", authorizationUrl);

    // 카카오 로그인 페이지로 리다이렉트
    response.sendRedirect(authorizationUrl);
  }

  /**
   * 카카오 로그인 콜백 처리 (하이브리드 방식 - 웹/모바일 구분)
   * 카카오 인증 후 Authorization Code를 받아 JWT 발급
   * 클라이언트 타입에 따라 토큰 전송 방식을 다르게 처리:
   * - 웹 브라우저: Refresh Token을 HTTP-only 쿠키로 전송하고 프론트엔드로 리다이렉트 (XSS 방어)
   * - 모바일 앱: 모든 토큰을 JSON 응답 바디로 전송
   *
   * GET /auth/kakao/callback?code=AUTHORIZATION_CODE
   *
   * @param code 카카오 인가 코드
   * @param request HTTP 요청 객체 (클라이언트 타입 감지용)
   * @param response HTTP 응답 객체 (쿠키 설정 및 리다이렉트용)
   * @return 모바일: 로그인 응답 (JWT 포함) / 웹: 프론트엔드로 리다이렉트
   */
  @GetMapping("/callback")
  public void kakaoCallback(
      @RequestParam String code,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    log.info("카카오 로그인 콜백 - code: {}", code);

    try {
      // 1️⃣ 클라이언트 타입 감지
      boolean isWebClient = ClientTypeDetector.isWebClient(request);
      String clientType = ClientTypeDetector.getClientTypeString(request);
      log.info("감지된 클라이언트 타입: {}", clientType);

      // 2️⃣ Authorization Code로 카카오 Access Token 요청
      KakaoOAuthDto.TokenResponse tokenResponse = kakaoOAuthService.getAccessToken(code);
      log.info("카카오 Access Token 발급 완료");

      // 3️⃣ 카카오 Access Token으로 사용자 정보 조회
      KakaoOAuthDto.UserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());
      log.info("카카오 사용자 정보 조회 완료 - 카카오 ID: {}", kakaoUserInfo.getId());

      // 4️⃣ 카카오 사용자 정보로 로그인 처리 (자동 회원가입 포함)
      LoginResponse loginResponse = kakaoOAuthService.processKakaoLogin(kakaoUserInfo);
      log.info("카카오 로그인 성공 - User ID: {}", loginResponse.getUser().getId());

      // 5️⃣ 웹 클라이언트면 Refresh Token을 쿠키로 설정하고 프론트엔드로 리다이렉트
      if (isWebClient) {
        log.info("웹 클라이언트 감지 → Refresh Token을 HTTP-only 쿠키로 설정하고 프론트엔드로 리다이렉트");

        // Refresh Token을 HTTP-only 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);   // JavaScript 접근 불가 (XSS 방어)
        refreshTokenCookie.setSecure(appProperties.getCookie().isSecure());  // 환경별 동적 설정 (개발: false, 프로덕션: true)
        log.info("쿠키 Secure 플래그: {}", appProperties.getCookie().isSecure());
        refreshTokenCookie.setPath("/");        // 모든 경로에서 쿠키 전송
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일 (초 단위)

        response.addCookie(refreshTokenCookie);
        log.info("Refresh Token을 쿠키에 설정 완료");

        // 프론트엔드 리다이렉트 URL 생성 (Access Token을 URL 파라미터로 전달)
        String redirectUrl = String.format("%s?accessToken=%s",
            appProperties.getOauth().getKakaoRedirectUrl(),
            loginResponse.getAccessToken()
        );

        log.info("프론트엔드로 리다이렉트: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
      } else {
        // 6️⃣ 모바일 클라이언트면 JSON 응답 반환
        log.info("모바일 클라이언트 감지 → JSON 응답 반환");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // JSON 응답 작성
        String jsonResponse = String.format(
            "{\"success\":true,\"message\":\"카카오 로그인 성공\",\"data\":{\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"user\":{\"id\":%d,\"email\":\"%s\",\"name\":\"%s\"}}}",
            loginResponse.getAccessToken(),
            loginResponse.getRefreshToken(),
            loginResponse.getUser().getId(),
            loginResponse.getUser().getEmail(),
            loginResponse.getUser().getName()
        );
        response.getWriter().write(jsonResponse);
      }

      log.info("카카오 로그인 성공: {}, 클라이언트: {}", loginResponse.getUser().getEmail(), clientType);

    } catch (Exception e) {
      log.error("카카오 로그인 실패: {}", e.getMessage(), e);
      // 에러 발생 시 프론트엔드로 리다이렉트 (에러 메시지 포함)
      String errorRedirectUrl = String.format("%s?error=%s",
          appProperties.getOauth().getKakaoRedirectUrl(),
          java.net.URLEncoder.encode(e.getMessage(), "UTF-8")
      );
      response.sendRedirect(errorRedirectUrl);
    }
  }
}
