package com.example.myauth.dto.admin;

import com.example.myauth.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

/**
 * 관리자 게시글 목록/상세 응답 DTO
 *
 * 【imageCount 처리】
 * - 목록 조회(findByAdminFilter): images가 LAZY이므로 초기화 안됨 → imageCount = null
 * - 상세 조회(findByIdForAdmin): LEFT JOIN FETCH로 images 로딩됨 → 실제 이미지 수 반환
 * - Hibernate.isInitialized()로 LAZY 컬렉션 접근 시 N+1 문제 방지
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPostListResponse {
  private Long id;
  private String content;
  private String visibility;
  private Integer likeCount;
  private Integer commentCount;
  private Integer viewCount;
  private Boolean isDeleted;
  private AdminAuthorInfo author;
  /** 이미지 수 (상세 조회 시에만 제공, 목록 조회 시 null) */
  private Integer imageCount;
  private LocalDateTime createdAt;

  /**
   * 관리자 게시글의 작성자 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminAuthorInfo {
    private Long id;
    private String name;
    private String email;
  }

  /**
   * Post 엔티티를 관리자 응답 DTO로 변환
   * LAZY 컬렉션(images)이 초기화되지 않은 경우 안전하게 null 처리
   *
   * @param post 게시글 엔티티
   * @return AdminPostListResponse DTO
   */
  public static AdminPostListResponse from(Post post) {
    // 본문 미리보기: 200자 초과 시 잘라서 표시
    String preview = post.getContent();
    if (preview != null && preview.length() > 200) {
      preview = preview.substring(0, 200) + "...";
    }

    // LAZY 컬렉션(images) 안전 접근
    // JOIN FETCH로 이미 로딩된 경우에만 size() 호출 (N+1 방지)
    Integer imgCount = null;
    if (post.getImages() != null && Hibernate.isInitialized(post.getImages())) {
      imgCount = post.getImages().size();
    }

    return AdminPostListResponse.builder()
        .id(post.getId())
        .content(preview)
        .visibility(post.getVisibility() != null ? post.getVisibility().name() : null)
        .likeCount(post.getLikeCount())
        .commentCount(post.getCommentCount())
        .viewCount(post.getViewCount())
        .isDeleted(post.getIsDeleted())
        .author(AdminAuthorInfo.builder()
            .id(post.getUser().getId())
            .name(post.getUser().getName())
            .email(post.getUser().getEmail())
            .build())
        .imageCount(imgCount)
        .createdAt(post.getCreatedAt())
        .build();
  }
}
