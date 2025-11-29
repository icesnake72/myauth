package com.example.myauth.repository;

import com.example.myauth.entity.RefreshToken;
import com.example.myauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token Repository
 * Refresh Token의 데이터베이스 작업을 처리한다
 */
@Repository
@SuppressWarnings("NullableProblems")  // JpaRepository 제네릭 타입은 항상 non-null
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * 토큰 문자열로 Refresh Token을 조회한다
   * @param token 토큰 문자열
   * @return RefreshToken (Optional)
   */
  Optional<RefreshToken> findByToken(String token);

  /**
   * 사용자의 모든 Refresh Token을 조회한다
   * @param user 사용자
   * @return RefreshToken 리스트
   */
  List<RefreshToken> findByUser(User user);

  /**
   * 사용자의 유효한 Refresh Token을 조회한다
   * @param user 사용자
   * @param now 현재 시간
   * @return 유효한 RefreshToken 리스트
   */
  @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now")
  List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

  /**
   * 사용자 ID로 모든 Refresh Token을 조회한다
   * @param userId 사용자 ID
   * @return RefreshToken 리스트
   */
  List<RefreshToken> findByUserId(Long userId);

  /**
   * 사용자의 모든 Refresh Token을 취소한다 (로그아웃 시 사용)
   * @param user 사용자
   * @return 업데이트된 행 수
   */
  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user AND rt.isRevoked = false")
  int revokeAllByUser(@Param("user") User user);

  /**
   * 사용자 ID로 모든 Refresh Token을 취소한다
   * @param userId 사용자 ID
   * @return 업데이트된 행 수
   */
  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user.id = :userId AND rt.isRevoked = false")
  int revokeAllByUserId(@Param("userId") Long userId);

  /**
   * 사용자 이메일로 모든 Refresh Token을 삭제한다 (로그아웃 시 사용)
   * @param email 사용자 이메일
   * @return 삭제된 행 수
   */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.user.email = :email")
  int deleteByUserEmail(@Param("email") String email);

  /**
   * 만료된 토큰을 삭제한다 (배치 작업에 사용)
   * @param now 현재 시간
   * @return 삭제된 행 수
   */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
  int deleteExpiredTokens(@Param("now") LocalDateTime now);

  /**
   * 취소된 토큰을 삭제한다 (배치 작업에 사용)
   * @return 삭제된 행 수
   */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true")
  int deleteRevokedTokens();

  /**
   * 토큰 존재 여부 확인
   * @param token 토큰 문자열
   * @return 존재하면 true
   */
  boolean existsByToken(String token);

  /**
   * 사용자의 활성 토큰 개수 조회
   * @param user 사용자
   * @return 활성 토큰 개수
   */
  @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now")
  long countActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}