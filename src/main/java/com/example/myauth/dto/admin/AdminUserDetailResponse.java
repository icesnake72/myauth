package com.example.myauth.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
  private Long id;
  private String email;
  private String name;
  private String profileImage;
  private String role;
  private String status;
  private Boolean isActive;
  private Boolean isSuperUser;
  private String provider;
  private LocalDateTime createdAt;
  private LocalDateTime lastLoginAt;
  private Integer failedLoginAttempts;
  private ProfileInfo profile;
  private UserActivityStats stats;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProfileInfo {
    private String lastName;
    private String firstName;
    private String phoneNumber;
    private LocalDateTime birth;
    private String bgImage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserActivityStats {
    private long postCount;
    private long commentCount;
    private long likeCount;
    private long followerCount;
    private long followingCount;
    private long bookmarkCount;
  }
}
