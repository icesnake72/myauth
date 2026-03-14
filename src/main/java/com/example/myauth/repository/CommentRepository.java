package com.example.myauth.repository;

import com.example.myauth.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 리포지토리
 * 댓글 CRUD 및 계층적 댓글(대댓글) 조회 기능 제공
 *
 * 【주요 기능】
 * - 게시글별 댓글 목록 조회 (최상위 댓글만)
 * - 특정 댓글의 대댓글 목록 조회
 * - 댓글 좋아요 수 증가/감소
 * - 게시글의 댓글 수 카운팅
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  // ===== 기본 조회 =====

  /**
   * ID로 댓글 조회 (삭제되지 않은 것만)
   *
   * @param id 댓글 ID
   * @return 댓글 Optional
   */
  Optional<Comment> findByIdAndIsDeletedFalse(Long id);

  /**
   * ID로 댓글 조회 (작성자 정보 포함, N+1 방지)
   *
   * @param id 댓글 ID
   * @return 댓글 Optional (작성자 정보 포함)
   */
  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.user " +
      "JOIN FETCH c.post " +
      "WHERE c.id = :id AND c.isDeleted = false")
  Optional<Comment> findByIdWithUserAndPost(@Param("id") Long id);

  // ===== 게시글별 댓글 조회 =====

  /**
   * 게시글의 최상위 댓글 목록 조회 (대댓글 제외)
   * 작성 시간 오름차순 정렬
   *
   * @param postId 게시글 ID
   * @param pageable 페이지 정보
   * @return 최상위 댓글 페이지
   */
  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.user " +
      "WHERE c.post.id = :postId " +
      "AND c.parent IS NULL " +
      "AND c.isDeleted = false " +
      "ORDER BY c.createdAt ASC")
  Page<Comment> findRootCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

  /**
   * 게시글의 모든 댓글 목록 조회 (대댓글 포함, 평면적 구조)
   * 프론트엔드에서 parentId로 그룹핑하여 계층 구조 표현
   *
   * @param postId 게시글 ID
   * @param pageable 페이지 정보
   * @return 모든 댓글 페이지
   */
  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.user " +
      "WHERE c.post.id = :postId " +
      "AND c.isDeleted = false " +
      "ORDER BY c.createdAt ASC")
  Page<Comment> findAllCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

  /**
   * 게시글의 모든 댓글 목록 조회 (페이징 없이, 삭제된 댓글도 포함)
   * 대댓글이 있는 삭제된 댓글은 "삭제된 댓글입니다" 표시
   *
   * @param postId 게시글 ID
   * @return 모든 댓글 리스트
   */
  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.user " +
      "WHERE c.post.id = :postId " +
      "ORDER BY c.createdAt ASC")
  List<Comment> findAllByPostIdWithDeleted(@Param("postId") Long postId);

  // ===== 대댓글 조회 =====

  /**
   * 특정 댓글의 대댓글 목록 조회
   *
   * @param parentId 부모 댓글 ID
   * @return 대댓글 리스트
   */
  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.user " +
      "WHERE c.parent.id = :parentId " +
      "AND c.isDeleted = false " +
      "ORDER BY c.createdAt ASC")
  List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

  /**
   * 특정 댓글의 대댓글 개수 조회
   *
   * @param parentId 부모 댓글 ID
   * @return 대댓글 개수
   */
  @Query("SELECT COUNT(c) FROM Comment c " +
      "WHERE c.parent.id = :parentId AND c.isDeleted = false")
  long countRepliesByParentId(@Param("parentId") Long parentId);

  // ===== 사용자별 조회 =====

  /**
   * 특정 사용자가 작성한 댓글 목록 조회
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 사용자의 댓글 페이지
   */
  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.post " +
      "WHERE c.user.id = :userId " +
      "AND c.isDeleted = false " +
      "ORDER BY c.createdAt DESC")
  Page<Comment> findByUserId(@Param("userId") Long userId, Pageable pageable);

  // ===== 카운트 쿼리 =====

  /**
   * 게시글의 댓글 수 조회
   *
   * @param postId 게시글 ID
   * @return 댓글 수
   */
  @Query("SELECT COUNT(c) FROM Comment c " +
      "WHERE c.post.id = :postId AND c.isDeleted = false")
  long countByPostId(@Param("postId") Long postId);

  // ===== 좋아요 수 업데이트 =====

  /**
   * 댓글 좋아요 수 증가
   *
   * @param commentId 댓글 ID
   */
  @Modifying
  @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
  void incrementLikeCount(@Param("commentId") Long commentId);

  /**
   * 댓글 좋아요 수 감소
   *
   * @param commentId 댓글 ID
   */
  @Modifying
  @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 " +
      "WHERE c.id = :commentId AND c.likeCount > 0")
  void decrementLikeCount(@Param("commentId") Long commentId);

  long countByIsDeletedFalse();

  long countByUserIdAndIsDeletedFalse(Long userId);

  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.user " +
      "JOIN FETCH c.post " +
      "WHERE (:keyword IS NULL OR c.content LIKE %:keyword% OR c.user.name LIKE %:keyword%) " +
      "AND (:postId IS NULL OR c.post.id = :postId) " +
      "ORDER BY c.createdAt DESC")
  Page<Comment> findByAdminFilter(
      @Param("keyword") String keyword,
      @Param("postId") Long postId,
      Pageable pageable
  );
}
