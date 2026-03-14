package com.example.myauth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티
 * 사용자가 작성하는 게시글(피드)의 기본 정보를 저장
 *
 * 【테이블 정보】
 * - 테이블명: posts
 * - 주요 기능: 게시글 본문, 공개 범위, 좋아요/댓글 수 캐싱
 *
 * 【연관 관계】
 * - User: N:1 (여러 게시글이 한 사용자에게 속함)
 * - PostImage: 1:N (한 게시글에 여러 이미지)
 * - Comment: 1:N (한 게시글에 여러 댓글)
 * - PostHashtag: 1:N (한 게시글에 여러 해시태그 연결)
 * - Bookmark: 1:N (한 게시글이 여러 사용자에게 북마크됨)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert  // INSERT 시 null인 필드 제외 (DB 기본값 사용)
@DynamicUpdate  // UPDATE 시 변경된 필드만 포함
@Table(name = "posts", indexes = {
    // 특정 사용자의 게시글 조회용 인덱스
    @Index(name = "idx_user_id", columnList = "user_id"),
    // 최신순 정렬 조회용 인덱스
    @Index(name = "idx_created_at", columnList = "created_at DESC"),
    // 특정 사용자의 최신 게시글 조회용 복합 인덱스
    @Index(name = "idx_user_created", columnList = "user_id, created_at DESC")
})
public class Post {

  /**
   * 게시글 고유 식별자 (자동 증가)
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 작성자 - User 엔티티와 N:1 관계
   * 사용자 삭제 시 해당 사용자의 모든 게시글도 삭제됨 (CASCADE)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * 게시글 본문 내용
   * TEXT 타입으로 긴 내용 저장 가능
   */
  @Column(columnDefinition = "TEXT")
  private String content;

  /**
   * 공개 범위
   * - PUBLIC: 전체 공개
   * - PRIVATE: 비공개 (작성자만)
   * - FOLLOWERS: 팔로워에게만 공개
   */
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  @ColumnDefault("'PUBLIC'")
  @Builder.Default
  private Visibility visibility = Visibility.PUBLIC;

  /**
   * 좋아요 수 (캐싱용)
   * 매번 COUNT 쿼리를 실행하지 않고 이 필드를 조회
   * 좋아요 추가/삭제 시 트리거 또는 애플리케이션에서 갱신
   */
  @Column(name = "like_count")
  @ColumnDefault("0")
  @Builder.Default
  private Integer likeCount = 0;

  /**
   * 댓글 수 (캐싱용)
   * 매번 COUNT 쿼리를 실행하지 않고 이 필드를 조회
   */
  @Column(name = "comment_count")
  @ColumnDefault("0")
  @Builder.Default
  private Integer commentCount = 0;

  /**
   * 조회수
   * 게시글 상세 조회 시 증가
   */
  @Column(name = "view_count")
  @ColumnDefault("0")
  @Builder.Default
  private Integer viewCount = 0;

  /**
   * 삭제 여부 (Soft Delete)
   * - false(0): 활성 상태
   * - true(1): 삭제됨
   * 물리적 삭제 대신 이 필드로 논리적 삭제 처리
   */
  @Column(name = "is_deleted")
  @ColumnDefault("false")
  @Builder.Default
  private Boolean isDeleted = false;

  /**
   * 작성 일시 (자동 설정)
   */
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  /**
   * 수정 일시 (자동 갱신)
   */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // ===== 연관 관계 (양방향) =====

  /**
   * 게시글에 첨부된 이미지 목록
   * 게시글 삭제 시 이미지도 함께 삭제 (orphanRemoval)
   */
  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PostImage> images = new ArrayList<>();

  /**
   * 게시글의 댓글 목록
   * 게시글 삭제 시 댓글도 함께 삭제
   */
  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Comment> comments = new ArrayList<>();

  /**
   * 게시글의 해시태그 연결 목록
   */
  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PostHashtag> postHashtags = new ArrayList<>();

  /**
   * 게시글의 북마크 목록
   */
  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Bookmark> bookmarks = new ArrayList<>();

  // ===== 편의 메서드 =====

  /**
   * 이미지 추가
   */
  public void addImage(PostImage image) {
    images.add(image);
    image.setPost(this);
  }

  /**
   * 댓글 추가
   */
  public void addComment(Comment comment) {
    comments.add(comment);
    comment.setPost(this);
    this.commentCount++;
  }

  /**
   * 좋아요 수 증가
   */
  public void incrementLikeCount() {
    this.likeCount++;
  }

  /**
   * 좋아요 수 감소
   */
  public void decrementLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  /**
   * 조회수 증가
   */
  public void incrementViewCount() {
    this.viewCount++;
  }

  /**
   * 논리적 삭제 처리
   */
  public void softDelete() {
    this.isDeleted = true;
  }

  /**
   * 논리적 삭제 복구
   */
  public void restore() {
    this.isDeleted = false;
  }
}
