package com.example.myauth.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 카카오 OAuth API 응답 DTO 모음
 */
public class KakaoOAuthDto {

  /**
   * 카카오 토큰 응답 DTO
   * POST https://kauth.kakao.com/oauth/token
   */
  @Getter
  @Setter
  @ToString
  public static class TokenResponse {
    /** 액세스 토큰 */
    @JsonProperty("access_token")
    private String accessToken;

    /** 토큰 타입 (Bearer) */
    @JsonProperty("token_type")
    private String tokenType;

    /** 리프레시 토큰 (optional) */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** 액세스 토큰 만료 시간 (초) */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /** 리프레시 토큰 만료 시간 (초, optional) */
    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;

    /** 인증된 사용자의 정보 조회 권한 범위 (optional) */
    @JsonProperty("scope")
    private String scope;
  }

  /**
   * 카카오 사용자 정보 응답 DTO
   * GET https://kapi.kakao.com/v2/user/me
   */
  @Getter
  @Setter
  @ToString
  public static class UserInfoResponse {
    /** 회원번호 */
    @JsonProperty("id")
    private Long id;

    /** 서비스에 연결 완료된 시각 (UTC) */
    @JsonProperty("connected_at")
    private String connectedAt;

    /** 카카오계정 정보 */
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    /** 프로필 정보 (deprecated, kakao_account.profile 사용 권장) */
    @JsonProperty("properties")
    private Properties properties;
  }

  /**
   * 카카오계정 정보
   */
  @Getter
  @Setter
  @ToString
  public static class KakaoAccount {
    /** 프로필 정보 */
    @JsonProperty("profile")
    private Profile profile;

    /** 이메일 */
    @JsonProperty("email")
    private String email;

    /** 이메일 제공 동의 여부 */
    @JsonProperty("has_email")
    private Boolean hasEmail;

    /** 이메일 인증 여부 */
    @JsonProperty("is_email_valid")
    private Boolean isEmailValid;

    /** 이메일 인증 여부 */
    @JsonProperty("is_email_verified")
    private Boolean isEmailVerified;

    /** 연령대 */
    @JsonProperty("age_range")
    private String ageRange;

    /** 생일 (MMDD) */
    @JsonProperty("birthday")
    private String birthday;

    /** 성별 (female/male) */
    @JsonProperty("gender")
    private String gender;
  }

  /**
   * 프로필 정보
   */
  @Getter
  @Setter
  @ToString
  public static class Profile {
    /** 닉네임 */
    @JsonProperty("nickname")
    private String nickname;

    /** 프로필 이미지 URL (640x640) */
    @JsonProperty("profile_image_url")
    private String profileImageUrl;

    /** 프로필 미리보기 이미지 URL (110x110) */
    @JsonProperty("thumbnail_image_url")
    private String thumbnailImageUrl;

    /** 프로필 이미지 URL 기본 이미지 여부 */
    @JsonProperty("is_default_image")
    private Boolean isDefaultImage;
  }

  /**
   * Properties (deprecated, 호환성 유지용)
   */
  @Getter
  @Setter
  @ToString
  public static class Properties {
    /** 닉네임 */
    @JsonProperty("nickname")
    private String nickname;

    /** 프로필 이미지 URL */
    @JsonProperty("profile_image")
    private String profileImage;

    /** 프로필 미리보기 이미지 URL */
    @JsonProperty("thumbnail_image")
    private String thumbnailImage;
  }
}
