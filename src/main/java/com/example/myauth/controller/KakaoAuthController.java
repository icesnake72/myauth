package com.example.myauth.controller;

import com.example.myauth.dto.kakao.KakaoOAuthDto;
import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.service.KakaoOAuthService;
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
   * 카카오 로그인 콜백 처리
   * 카카오 인증 후 Authorization Code를 받아 JWT 발급
   *
   * GET /auth/kakao/callback?code=AUTHORIZATION_CODE
   *
   * @param code 카카오 인가 코드
   * @return 로그인 응답 (JWT 포함)
   */
  @GetMapping("/callback")
  public ResponseEntity<ApiResponse<LoginResponse>> kakaoCallback(@RequestParam String code) {
    log.info("카카오 로그인 콜백 - code: {}", code);

    try {
      // 1️⃣ Authorization Code로 카카오 Access Token 요청
      KakaoOAuthDto.TokenResponse tokenResponse = kakaoOAuthService.getAccessToken(code);
      log.info("카카오 Access Token 발급 완료");

      // 2️⃣ 카카오 Access Token으로 사용자 정보 조회
      KakaoOAuthDto.UserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());
      log.info("카카오 사용자 정보 조회 완료 - 카카오 ID: {}", kakaoUserInfo.getId());

      // 3️⃣ 카카오 사용자 정보로 로그인 처리 (자동 회원가입 포함)
      LoginResponse loginResponse = kakaoOAuthService.processKakaoLogin(kakaoUserInfo);
      log.info("카카오 로그인 성공 - User ID: {}", loginResponse.getUser().getId());

      return ResponseEntity.ok(ApiResponse.success("카카오 로그인 성공", loginResponse));

    } catch (Exception e) {
      log.error("카카오 로그인 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body(ApiResponse.error("카카오 로그인 실패: " + e.getMessage()));
    }
  }
}
