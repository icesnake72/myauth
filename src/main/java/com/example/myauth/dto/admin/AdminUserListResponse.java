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
public class AdminUserListResponse {
  private Long id;
  private String email;
  private String name;
  private String profileImage;
  private String role;
  private String status;
  private Boolean isActive;
  private String provider;
  private long postCount;
  private long followerCount;
  private LocalDateTime createdAt;
  private LocalDateTime lastLoginAt;
}
