package com.example.myauth.repository;

import com.example.myauth.entity.Like;
import com.example.myauth.entity.TargetType;
import com.example.myauth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 좋아요 리포지토리
 * 게시글/댓글에 대한 좋아요 관리
 * 다형성 연관 관계를 통해 하나의 테이블에서 관리
 *
 * 【주요 기능】
 * - 좋아요 추가/삭제
 * - 좋아요 여부 확인
 * - 좋아요 누른 사용자 목록 조회
 * - 좋아요 수 조회
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

  // ===== 좋아요 존재 여부 확인 =====

  /**
   * 특정 사용자가 특정 대상에 좋아요 했는지 확인
   *
   * @param userId 사용자 ID
   * @param targetType 대상 유형 (POST/COMMENT)
   * @param targetId 대상 ID
   * @return 좋아요 여부
   */
  boolean existsByUserIdAndTargetTypeAndTargetId(
      Long userId, TargetType targetType, Long targetId);

  /**
   * 특정 사용자가 게시글에 좋아요 했는지 확인
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 좋아요 여부
   */
  default boolean existsPostLikeByUserId(Long userId, Long postId) {
    return existsByUserIdAndTargetTypeAndTargetId(userId, TargetType.POST, postId);
  }

  /**
   * 특정 사용자가 댓글에 좋아요 했는지 확인
   *
   * @param userId 사용자 ID
   * @param commentId 댓글 ID
   * @return 좋아요 여부
   */
  default boolean existsCommentLikeByUserId(Long userId, Long commentId) {
    return existsByUserIdAndTargetTypeAndTargetId(userId, TargetType.COMMENT, commentId);
  }

  // ===== 좋아요 조회 =====

  /**
   * 특정 사용자의 특정 대상에 대한 좋아요 조회
   *
   * @param userId 사용자 ID
   * @param targetType 대상 유형
   * @param targetId 대상 ID
   * @return 좋아요 Optional
   */
  Optional<Like> findByUserIdAndTargetTypeAndTargetId(
      Long userId, TargetType targetType, Long targetId);

  /**
   * 게시글에 대한 사용자의 좋아요 조회
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 좋아요 Optional
   */
  default Optional<Like> findPostLikeByUserId(Long userId, Long postId) {
    return findByUserIdAndTargetTypeAndTargetId(userId, TargetType.POST, postId);
  }

  /**
   * 댓글에 대한 사용자의 좋아요 조회
   *
   * @param userId 사용자 ID
   * @param commentId 댓글 ID
   * @return 좋아요 Optional
   */
  default Optional<Like> findCommentLikeByUserId(Long userId, Long commentId) {
    return findByUserIdAndTargetTypeAndTargetId(userId, TargetType.COMMENT, commentId);
  }

  // ===== 좋아요 삭제 =====

  /**
   * 특정 사용자의 특정 대상에 대한 좋아요 삭제
   *
   * @param userId 사용자 ID
   * @param targetType 대상 유형
   * @param targetId 대상 ID
   */
  void deleteByUserIdAndTargetTypeAndTargetId(
      Long userId, TargetType targetType, Long targetId);

  // ===== 좋아요 누른 사용자 목록 조회 =====

  /**
   * 특정 대상에 좋아요한 사용자 목록 조회
   *
   * @param targetType 대상 유형
   * @param targetId 대상 ID
   * @param pageable 페이지 정보
   * @return 좋아요한 사용자 페이지
   */
  @Query("SELECT l.user FROM Like l " +
      "WHERE l.targetType = :targetType AND l.targetId = :targetId " +
      "ORDER BY l.createdAt DESC")
  Page<User> findUsersByTargetTypeAndTargetId(
      @Param("targetType") TargetType targetType,
      @Param("targetId") Long targetId,
      Pageable pageable);

  /**
   * 게시글에 좋아요한 사용자 목록 조회
   *
   * @param postId 게시글 ID
   * @param pageable 페이지 정보
   * @return 좋아요한 사용자 페이지
   */
  default Page<User> findUsersWhoLikedPost(Long postId, Pageable pageable) {
    return findUsersByTargetTypeAndTargetId(TargetType.POST, postId, pageable);
  }

  /**
   * 댓글에 좋아요한 사용자 목록 조회
   *
   * @param commentId 댓글 ID
   * @param pageable 페이지 정보
   * @return 좋아요한 사용자 페이지
   */
  default Page<User> findUsersWhoLikedComment(Long commentId, Pageable pageable) {
    return findUsersByTargetTypeAndTargetId(TargetType.COMMENT, commentId, pageable);
  }

  // ===== 좋아요 수 조회 =====

  /**
   * 특정 대상의 좋아요 수 조회
   *
   * @param targetType 대상 유형
   * @param targetId 대상 ID
   * @return 좋아요 수
   */
  long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

  /**
   * 게시글의 좋아요 수 조회
   *
   * @param postId 게시글 ID
   * @return 좋아요 수
   */
  default long countPostLikes(Long postId) {
    return countByTargetTypeAndTargetId(TargetType.POST, postId);
  }

  /**
   * 댓글의 좋아요 수 조회
   *
   * @param commentId 댓글 ID
   * @return 좋아요 수
   */
  default long countCommentLikes(Long commentId) {
    return countByTargetTypeAndTargetId(TargetType.COMMENT, commentId);
  }

  // ===== 사용자별 좋아요 목록 =====

  /**
   * 특정 사용자가 좋아요한 게시글 ID 목록 조회
   *
   * @param userId 사용자 ID
   * @param postIds 확인할 게시글 ID 목록
   * @return 좋아요한 게시글 ID 목록
   */
  @Query("SELECT l.targetId FROM Like l " +
      "WHERE l.user.id = :userId " +
      "AND l.targetType = 'POST' " +
      "AND l.targetId IN :postIds")
  List<Long> findLikedPostIdsByUserId(
      @Param("userId") Long userId,
      @Param("postIds") List<Long> postIds);

  /**
   * 특정 사용자가 좋아요한 댓글 ID 목록 조회
   *
   * @param userId 사용자 ID
   * @param commentIds 확인할 댓글 ID 목록
   * @return 좋아요한 댓글 ID 목록
   */
  @Query("SELECT l.targetId FROM Like l " +
      "WHERE l.user.id = :userId " +
      "AND l.targetType = 'COMMENT' " +
      "AND l.targetId IN :commentIds")
  List<Long> findLikedCommentIdsByUserId(
      @Param("userId") Long userId,
      @Param("commentIds") List<Long> commentIds);

  long countByUserId(Long userId);
}
