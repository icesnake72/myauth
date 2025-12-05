package com.example.myauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth 설정 프로퍼티
 * application.yml의 oauth.kakao 설정을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoOAuthProperties {

  /** 카카오 REST API 키 (Client ID) */
  private String clientId;

  /** 카카오 Client Secret */
  private String clientSecret;

  /** 카카오 인증 후 리다이렉트 URI */
  private String redirectUri;

  /** 카카오 인가 코드 요청 URI */
  private String authorizationUri;

  /** 카카오 액세스 토큰 요청 URI */
  private String tokenUri;

  /** 카카오 사용자 정보 조회 URI */
  private String userInfoUri;
}
