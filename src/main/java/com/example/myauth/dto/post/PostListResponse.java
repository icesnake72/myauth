package com.example.myauth.dto.post;

import com.example.myauth.entity.Post;
import com.example.myauth.entity.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 목록 응답 DTO
 * 게시글 목록 조회 시 반환되는 간략한 정보
 * (상세 조회보다 적은 정보 포함 - 성능 최적화)
 *
 * 【응답 예시】
 * {
 *   "id": 1,
 *   "content": "오늘 맛있는 저녁...",
 *   "thumbnailUrl": "http://...",
 *   "imageCount": 3,
 *   "likeCount": 42,
 *   "commentCount": 5,
 *   "author": {
 *     "id": 1,
 *     "name": "홍길동",
 *     "profileImage": "http://..."
 *   },
 *   "createdAt": "2025-01-24T10:30:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostListResponse {

  /**
   * 게시글 ID
   */
  private Long id;

  /**
   * 게시글 본문 (미리보기용 - 최대 100자)
   */
  private String content;

  /**
   * 공개 범위
   */
  private Visibility visibility;

  /**
   * 대표 이미지 URL (첫 번째 이미지의 썸네일)
   */
  private String thumbnailUrl;

  /**
   * 첨부 이미지 개수
   */
  private Integer imageCount;

  /**
   * 좋아요 수
   */
  private Integer likeCount;

  /**
   * 댓글 수
   */
  private Integer commentCount;

  /**
   * 조회수
   */
  private Integer viewCount;

  /**
   * 작성자 정보
   */
  private PostAuthorResponse author;

  /**
   * 작성 일시
   */
  private LocalDateTime createdAt;

  /**
   * Entity → DTO 변환
   */
  public static PostListResponse from(Post post) {
    // 본문 미리보기 (최대 100자)
    String contentPreview = post.getContent();
    if (contentPreview != null && contentPreview.length() > 100) {
      contentPreview = contentPreview.substring(0, 100) + "...";
    }

    // 첫 번째 이미지의 썸네일 URL
    String thumbnailUrl = null;
    if (!post.getImages().isEmpty()) {
      thumbnailUrl = post.getImages().get(0).getThumbnailUrl();
      // 썸네일이 없으면 원본 이미지 URL 사용
      if (thumbnailUrl == null) {
        thumbnailUrl = post.getImages().get(0).getImageUrl();
      }
    }

    return PostListResponse.builder()
        .id(post.getId())
        .content(contentPreview)
        .visibility(post.getVisibility())
        .thumbnailUrl(thumbnailUrl)
        .imageCount(post.getImages().size())
        .likeCount(post.getLikeCount())
        .commentCount(post.getCommentCount())
        .viewCount(post.getViewCount())
        .author(PostAuthorResponse.from(post.getUser()))
        .createdAt(post.getCreatedAt())
        .build();
  }
}
