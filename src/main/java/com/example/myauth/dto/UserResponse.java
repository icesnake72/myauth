package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 * 민감한 정보(비밀번호 등)는 제외하고 필요한 정보만 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
  /**
   * 사용자 ID
   */
  private Long id;

  /**
   * 이메일
   */
  private String email;

  /**
   * 이름
   */
  private String name;

  /**
   * 프로필 이미지 URL
   */
  private String profileImage;

  /**
   * OAuth 제공자 (KAKAO, GOOGLE 등)
   */
  private String provider;

  /**
   * 권한 (ROLE_USER, ROLE_ADMIN)
   */
  private String role;

  /**
   * 계정 상태 (ACTIVE, INACTIVE, SUSPENDED)
   */
  private String status;

  /**
   * 활성화 여부
   */
  private Boolean isActive;

  /**
   * 생성 일시
   */
  private LocalDateTime createdAt;
}
