package com.example.myauth.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**  이메일 (필수 필드 - 카카오 개발자 콘솔에서 필수 동의 항목으로 설정) */
  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(nullable = false, length = 255)
  private String name;

  /** 비밀번호 (OAuth 로그인 시 불필요하므로 nullable) */
  @Column(length = 255)
  private String password;

  /** enum('ROLE_ADMIN','ROLE_USER') DEFAULT 'ROLE_USER' */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'ROLE_USER'")
  @Builder.Default
  private Role role = Role.ROLE_USER;

  /** timestamp DEFAULT CURRENT_TIMESTAMP */
  @CreationTimestamp  // Hibernate가 자동으로 생성 시간 설정
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  /** timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP */
  @UpdateTimestamp  // Hibernate가 자동으로 수정 시간 설정
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** tinyint(1) NOT NULL DEFAULT 1 */
  @Column(name = "is_active", nullable = false)
  @ColumnDefault("1")
  @Builder.Default
  private Boolean isActive = true;

  /** tinyint(1) DEFAULT 0 */
  @Column(name = "is_super_user")
  @ColumnDefault("0")
  @Builder.Default
  private Boolean isSuperUser = false;

  @Column(name = "account_locked_until")
  private LocalDateTime accountLockedUntil;

  @Column(name = "email_verified_at")
  private LocalDateTime emailVerifiedAt;

  /** int NOT NULL DEFAULT 0 */
  @Column(name = "failed_login_attempts", nullable = false)
  @ColumnDefault("0")
  @Builder.Default
  private Integer failedLoginAttempts = 0;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "last_login_ip", length = 45)
  private String lastLoginIp;

  /** enum('ACTIVE','DELETED','INACTIVE','PENDING_VERIFICATION','SUSPENDED') DEFAULT 'ACTIVE' */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'ACTIVE'")
  @Builder.Default
  private Status status = Status.ACTIVE;

  // ----- OAuth 관련 필드 -----

  /** 로그인 제공자 (LOCAL, KAKAO, GOOGLE 등) */
  @Column(length = 20)
  @ColumnDefault("'LOCAL'")
  @Builder.Default
  private String provider = "LOCAL";

  /** OAuth 제공자의 고유 ID (카카오 회원번호 등) */
  @Column(name = "provider_id", length = 100)
  private String providerId;

  /** 프로필 이미지 URL */
  @Column(name = "profile_image", length = 500)
  private String profileImage;

  // ----- ENUMS -----
  public enum Role {
    ROLE_ADMIN,
    ROLE_USER
  }

  public enum Status {
    ACTIVE,
    DELETED,
    INACTIVE,
    PENDING_VERIFICATION,
    SUSPENDED
  }
}
